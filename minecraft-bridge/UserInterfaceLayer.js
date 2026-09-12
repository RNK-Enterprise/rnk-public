const readline = require('readline');

class UserInterfaceLayer {
    constructor(bridge) {
        this.bridge = bridge;
        this.rl = null;
    }

    ensureInterface() {
        if (!this.rl) {
            this.rl = readline.createInterface({
                input: process.stdin,
                output: process.stdout
            });
        }
    }

    async start() {
        this.ensureInterface();
        this.clearScreen();
        this.showBanner();

        console.log('Scanning for Minecraft servers...');

        try {
            await this.bridge.initialize();
            const servers = await this.bridge.serverLauncher.detectServers();

            if (servers.length === 0) {
                console.log('No Minecraft servers detected.');
                console.log('Common locations:');
                console.log('  - Desktop/minecraft-server');
                console.log('  - Documents/minecraft-server');
                console.log('  - C:\\minecraft-server');
                console.log('  - /opt/minecraft-server');
                console.log('  - ~/minecraft-server');
                console.log('');
                console.log('You can also import the temporary Curator download link from your email.');
                await this.promptForCuratorDownloadLink();
                return;
            }

            console.log(`Found ${servers.length} server(s):`);
            console.log('');
            servers.forEach((server, index) => {
                const type = this.detectServerType(server.jarFile);
                console.log(`  ${index + 1}. ${this.getServerTypeIcon(type)} ${server.name}`);
                console.log(`     Path: ${server.path}`);
                console.log(`     Jar:  ${server.jarFile}`);
                console.log('');
            });

            await this.showMainMenu(servers);
        } catch (error) {
            console.error('Error during initialization:', error.message);
            process.exit(1);
        }
    }

    async showMainMenu(servers) {
        this.ensureInterface();

        console.log('Main Menu');
        console.log('  1..N Launch a detected server');
        console.log('  I   Import a Curator JAR');
        console.log('  U   Import a Curator download link');
        console.log('  M   Enter a manual server path');
        console.log('  R   Refresh detection');
        console.log('  S   Show system status');
        console.log('  Q   Quit');
        console.log('');

        const answer = (await this.question('Select an option: ')).trim();

        if (answer.toLowerCase() === 'q') {
            await this.bridge.shutdown();
            process.exit(0);
        }

        if (answer.toLowerCase() === 'i') {
            await this.promptForCuratorJar();
            await this.showMainMenu(servers);
            return;
        }

        if (answer.toLowerCase() === 'm') {
            await this.promptForManualServerPath();
            return;
        }

        if (answer.toLowerCase() === 'u') {
            await this.promptForCuratorDownloadLink();
            return;
        }

        if (answer.toLowerCase() === 'r') {
            await this.start();
            return;
        }

        if (answer.toLowerCase() === 's') {
            await this.showSystemStatus();
            await this.showMainMenu(servers);
            return;
        }

        const serverIndex = Number.parseInt(answer, 10) - 1;
        if (Number.isInteger(serverIndex) && serverIndex >= 0 && serverIndex < servers.length) {
            await this.launchServerWithMods(servers[serverIndex]);
            return;
        }

        console.log('Invalid option.');
        console.log('');
        await this.showMainMenu(servers);
    }

    async promptForCuratorJar() {
        const jarPath = await this.question('Enter the full path to the Curator output JAR: ');

        try {
            const result = await this.bridge.receiveJar(jarPath.trim());
            console.log(`Saved JAR to: ${result.receivedPath}`);
        } catch (error) {
            console.log(`Could not import JAR: ${error.message}`);
        }
    }

    async promptForCuratorDownloadLink() {
        const downloadUrl = await this.question('Enter the Curator temporary download link: ');

        try {
            const result = await this.bridge.receiveJarFromUrl(downloadUrl.trim());
            console.log(`Imported JAR to: ${result.receivedPath}`);
        } catch (error) {
            console.log(`Could not import from link: ${error.message}`);
        }
    }

    async launchServerWithMods(server) {
        const type = this.detectServerType(server.jarFile);

        console.log('Launching server...');
        console.log(`Server: ${server.name}`);
        console.log(`Type:   ${type.toUpperCase()}`);
        console.log(`Path:   ${server.path}`);
        console.log('');

        try {
            const serverInfo = await this.bridge.serverLauncher.launchServer(
                server.path,
                server.jarFile,
                { minRam: '1G', maxRam: '4G' }
            );

            console.log(`Server started (PID: ${serverInfo.process.pid})`);
            console.log('');

            const modPackage = await this.bridge.modInjector.getUniversalMods(
                '1.20.1',
                type,
                this.bridge.latestJar
            );

            const injectionResult = await this.bridge.modInjector.injectModsIntoServer(
                serverInfo.id,
                modPackage,
                serverInfo.process.pid
            );

            if (injectionResult.success) {
                console.log('Universal mod handoff complete.');
            } else {
                console.log('Mod handoff failed; server is still running.');
            }

            console.log('Press Ctrl+C to stop the server and return to the menu.');

            const originalSigint = process.listeners('SIGINT')[0];
            if (originalSigint) {
                process.removeListener('SIGINT', originalSigint);
            }

            await new Promise((resolve) => {
                process.once('SIGINT', async () => {
                    try {
                        await this.bridge.serverLauncher.stopServer(serverInfo.id);
                        console.log('Server stopped.');
                    } catch (error) {
                        console.error(`Error stopping server: ${error.message}`);
                    }
                    resolve();
                });
            });

            if (originalSigint) {
                process.addListener('SIGINT', originalSigint);
            }
        } catch (error) {
            console.error(`Error during launch: ${error.message}`);
        }

        console.log('');
        await this.question('Press Enter to return to the main menu...');
        await this.start();
    }

    async promptForManualServerPath() {
        const serverPath = (await this.question('Enter the full path to your Minecraft server directory: ')).trim();

        try {
            const stats = await require('fs').promises.stat(serverPath);
            if (!stats.isDirectory()) {
                throw new Error('Path is not a directory');
            }

            const jarFile = await this.bridge.serverLauncher.checkForServerJar(serverPath);
            if (!jarFile) {
                console.log('No Minecraft server JAR found in that directory.');
                await this.promptForManualServerPath();
                return;
            }

            await this.launchServerWithMods({
                path: serverPath,
                name: require('path').basename(serverPath),
                jarFile
            });
        } catch (error) {
            console.log(`Invalid path: ${error.message}`);
            await this.promptForManualServerPath();
        }
    }

    detectServerType(jarFile) {
        const lowerJar = String(jarFile).toLowerCase();
        if (lowerJar.includes('paper')) return 'paper';
        if (lowerJar.includes('spigot')) return 'spigot';
        if (lowerJar.includes('bukkit')) return 'bukkit';
        if (lowerJar.includes('forge')) return 'forge';
        if (lowerJar.includes('fabric')) return 'fabric';
        return 'vanilla';
    }

    question(question) {
        this.ensureInterface();
        return new Promise((resolve) => {
            this.rl.question(question, (answer) => resolve(answer));
        });
    }

    clearScreen() {
        console.clear();
    }

    showBanner() {
        console.log('==============================================================');
        console.log('RNK MINECRAFT BRIDGE');
        console.log('Downloader bridge for Curator output jars');
        console.log('==============================================================');
        console.log('');
    }

    getServerTypeIcon(type) {
        const icons = {
            paper: '[paper]',
            spigot: '[spigot]',
            bukkit: '[bukkit]',
            forge: '[forge]',
            fabric: '[fabric]',
            vanilla: '[vanilla]'
        };
        return icons[type] || '[server]';
    }

    async showSystemStatus() {
        console.log('System Status');
        console.log('');

        const status = this.bridge.getStatus();
        const runningServers = this.bridge.serverLauncher.getRunningServers();

        console.log(`Bridge:         ${status.initialized ? 'initialized' : 'not initialized'}`);
        console.log(`Tync:           ${status.tyncConnected ? 'connected' : 'disconnected'}`);
        console.log(`Running servers:${runningServers.length}`);
        console.log(`Version:        ${status.version}`);
        console.log(`Inbox:          ${status.inboxDir}`);
        console.log(`Latest JAR:     ${status.latestJar || 'none'}`);
        console.log('');

        const javaCheck = await new Promise((resolve) => {
            const { spawn } = require('child_process');
            const java = spawn('java', ['-version'], { stdio: 'pipe' });
            java.on('close', (code) => resolve(code === 0));
            java.on('error', () => resolve(false));
        });

        console.log(`Java:           ${javaCheck ? 'installed' : 'not found'}`);
        console.log('');
        await this.question('Press Enter to continue...');
    }

    cleanup() {
        if (this.rl) {
            this.rl.close();
            this.rl = null;
        }
    }
}

module.exports = UserInterfaceLayer;
