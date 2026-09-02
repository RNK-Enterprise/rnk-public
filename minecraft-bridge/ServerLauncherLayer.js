const { spawn } = require('child_process');
const fs = require('fs').promises;
const path = require('path');
const os = require('os');

class ServerLauncherLayer {
    constructor() {
        this.runningServers = new Map();
        this.commonServerPaths = this.getCommonServerPaths();
    }

    getCommonServerPaths() {
        const home = os.homedir();
        if (os.platform() === 'win32') {
            return [
                path.join(home, 'Desktop', 'minecraft-server'),
                path.join(home, 'Documents', 'minecraft-server'),
                'C:\\minecraft-server',
                'D:\\minecraft-server'
            ];
        }

        if (os.platform() === 'darwin') {
            return [
                path.join(home, 'Desktop', 'minecraft-server'),
                path.join(home, 'Documents', 'minecraft-server'),
                '/Applications/minecraft-server'
            ];
        }

        return [
            path.join(home, 'minecraft-server'),
            '/opt/minecraft-server',
            '/srv/minecraft-server'
        ];
    }

    async detectServers() {
        const detectedServers = [];

        for (const serverPath of this.commonServerPaths) {
            try {
                const stats = await fs.stat(serverPath);
                if (stats.isDirectory()) {
                    const jarFile = await this.checkForServerJar(serverPath);
                    if (jarFile) {
                        detectedServers.push({
                            path: serverPath,
                            name: path.basename(serverPath),
                            jarFile
                        });
                    }
                }
            } catch {
                // Ignore missing paths.
            }
        }

        const currentDirServers = await this.scanDirectoryForServers(process.cwd());
        detectedServers.push(...currentDirServers);

        return detectedServers;
    }

    async checkForServerJar(dirPath) {
        try {
            const files = await fs.readdir(dirPath);
            for (const file of files) {
                const lower = file.toLowerCase();
                if (lower.endsWith('.jar') && (lower.includes('server') || lower.includes('minecraft') || lower.includes('paper') || lower.includes('spigot') || lower.includes('bukkit') || lower.includes('forge'))) {
                    return file;
                }
            }
        } catch {
            // Directory not readable.
        }
        return null;
    }

    async scanDirectoryForServers(scanPath, depth = 0) {
        if (depth > 3) {
            return [];
        }

        const servers = [];
        try {
            const items = await fs.readdir(scanPath, { withFileTypes: true });
            for (const item of items) {
                if (!item.isDirectory()) {
                    continue;
                }

                const fullPath = path.join(scanPath, item.name);
                const jarFile = await this.checkForServerJar(fullPath);
                if (jarFile) {
                    servers.push({
                        path: fullPath,
                        name: item.name,
                        jarFile
                    });
                } else {
                    const nestedServers = await this.scanDirectoryForServers(fullPath, depth + 1);
                    servers.push(...nestedServers);
                }
            }
        } catch {
            // Ignore inaccessible directories.
        }

        return servers;
    }

    async launchServer(serverPath, jarFile, options = {}) {
        const { minRam = '1G', maxRam = '4G', serverArgs = [] } = options;
        const absoluteServerPath = path.resolve(serverPath);
        const jarPath = path.isAbsolute(jarFile) ? jarFile : path.join(absoluteServerPath, jarFile);

        const serverStats = await fs.stat(absoluteServerPath).catch(() => null);
        if (!serverStats || !serverStats.isDirectory()) {
            throw new Error(`Server path not found: ${absoluteServerPath}`);
        }

        const jarStats = await fs.stat(jarPath).catch(() => null);
        if (!jarStats || !jarStats.isFile()) {
            throw new Error(`Server JAR not found: ${jarPath}`);
        }

        return new Promise((resolve, reject) => {
            const javaArgsArray = [
                `-Xms${minRam}`,
                `-Xmx${maxRam}`,
                '-jar',
                path.basename(jarPath),
                'nogui',
                ...serverArgs
            ];

            const serverProcess = spawn('java', javaArgsArray, {
                cwd: absoluteServerPath,
                stdio: ['pipe', 'pipe', 'pipe']
            });

            const serverId = `server_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`;
            const serverInfo = {
                id: serverId,
                path: absoluteServerPath,
                jarFile: path.basename(jarPath),
                process: serverProcess,
                startTime: new Date(),
                status: 'starting'
            };

            this.runningServers.set(serverId, serverInfo);

            let startupComplete = false;
            const startupTimeout = setTimeout(() => {
                if (!startupComplete) {
                    serverProcess.kill();
                    this.runningServers.delete(serverId);
                    reject(new Error('Server startup timeout'));
                }
            }, 60000);

            serverProcess.stdout.on('data', (data) => {
                const output = data.toString();
                if (output.includes('Done') && output.includes('For help, type "help"')) {
                    startupComplete = true;
                    clearTimeout(startupTimeout);
                    serverInfo.status = 'running';
                    resolve(serverInfo);
                }
            });

            serverProcess.stderr.on('data', (data) => {
                console.error(`[${serverId}] ${data.toString().trim()}`);
            });

            serverProcess.on('close', (code) => {
                this.runningServers.delete(serverId);
                if (!startupComplete) {
                    clearTimeout(startupTimeout);
                    reject(new Error(`Server exited prematurely with code ${code}`));
                }
            });

            serverProcess.on('error', (error) => {
                clearTimeout(startupTimeout);
                this.runningServers.delete(serverId);
                reject(error);
            });
        });
    }

    async stopServer(serverId) {
        const serverInfo = this.runningServers.get(serverId);
        if (!serverInfo) {
            throw new Error(`Server ${serverId} not found`);
        }

        return new Promise((resolve) => {
            if (serverInfo.process && !serverInfo.process.killed) {
                serverInfo.process.stdin.write('stop\n');
                serverInfo.process.once('close', () => resolve(true));
                setTimeout(() => {
                    if (!serverInfo.process.killed) {
                        serverInfo.process.kill();
                    }
                    resolve(true);
                }, 30000);
            } else {
                resolve(false);
            }
        });
    }

    async stopAllServers() {
        const serverIds = Array.from(this.runningServers.keys());
        for (const serverId of serverIds) {
            try {
                await this.stopServer(serverId);
            } catch {
                // Ignore shutdown failures.
            }
        }
    }

    getRunningServers() {
        return Array.from(this.runningServers.values()).map((server) => ({
            id: server.id,
            path: server.path,
            jarFile: server.jarFile,
            status: server.status,
            startTime: server.startTime,
            pid: server.process ? server.process.pid : null
        }));
    }

    isServerReady(serverId) {
        const server = this.runningServers.get(serverId);
        return Boolean(server && server.status === 'running');
    }
}

module.exports = ServerLauncherLayer;
