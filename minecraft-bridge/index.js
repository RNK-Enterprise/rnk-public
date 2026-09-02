const path = require('path');
const os = require('os');
const fs = require('fs').promises;
const fsSync = require('fs');
const https = require('https');
const http = require('http');
const { URL } = require('url');

const TyncConnectionLayer = require('./TyncConnectionLayer');
const UniversalModInjectionLayer = require('./UniversalModInjectionLayer');
const ServerLauncherLayer = require('./ServerLauncherLayer');
const UserInterfaceLayer = require('./UserInterfaceLayer');

class LDMinecraftBridge {
    constructor(options = {}) {
        this.version = require('./package.json').version;
        this.options = options;
        this.initialized = false;
        this.receivedJarHistory = [];
        this.latestJar = null;
        this.inboxDir = options.inboxDir || path.join(os.homedir(), '.ld-minecraft-bridge', 'inbox');

        this.tyncConnection = new TyncConnectionLayer(options.tync);
        this.tync = this.tyncConnection;

        this.modInjector = new UniversalModInjectionLayer(this, options.modInjector);
        this.injector = this.modInjector;

        this.serverLauncher = new ServerLauncherLayer(options.serverLauncher);
        this.launcher = this.serverLauncher;

        this.userInterface = new UserInterfaceLayer(this);
    }

    async initialize() {
        if (this.initialized) {
            return this.getStatus();
        }

        await fs.mkdir(this.inboxDir, { recursive: true });
        await this.modInjector.ensureInjectorJar();
        this.initialized = true;

        return this.getStatus();
    }

    async receiveJar(sourcePath, options = {}) {
        if (!sourcePath) {
            throw new Error('A JAR path is required');
        }

        await this.initialize();

        const absoluteSource = path.resolve(sourcePath);
        const stats = await fs.stat(absoluteSource);
        if (!stats.isFile()) {
            throw new Error('The provided path is not a file');
        }
        if (!absoluteSource.toLowerCase().endsWith('.jar')) {
            throw new Error('The provided file is not a JAR');
        }

        const outputName = options.outputName || path.basename(absoluteSource);
        const safeName = outputName.replace(/[^a-zA-Z0-9._-]/g, '_');
        const destination = path.join(this.inboxDir, `${Date.now()}-${safeName}`);

        await fs.copyFile(absoluteSource, destination);

        const record = {
            sourcePath: absoluteSource,
            receivedPath: destination,
            receivedAt: new Date().toISOString(),
            size: stats.size
        };

        this.latestJar = record.receivedPath;
        this.receivedJarHistory.unshift(record);
        this.receivedJarHistory = this.receivedJarHistory.slice(0, 10);

        return record;
    }

    async receiveJarBuffer(buffer, filename = 'curator-output.jar') {
        if (!Buffer.isBuffer(buffer)) {
            throw new Error('A Buffer is required');
        }

        await this.initialize();

        const safeName = filename.replace(/[^a-zA-Z0-9._-]/g, '_');
        const destination = path.join(this.inboxDir, `${Date.now()}-${safeName}`);
        await fs.writeFile(destination, buffer);

        const record = {
            sourcePath: null,
            receivedPath: destination,
            receivedAt: new Date().toISOString(),
            size: buffer.length
        };

        this.latestJar = record.receivedPath;
        this.receivedJarHistory.unshift(record);
        this.receivedJarHistory = this.receivedJarHistory.slice(0, 10);

        return record;
    }

    async receiveJarFromUrl(downloadUrl, options = {}) {
        if (!downloadUrl) {
            throw new Error('A download URL is required');
        }

        await this.initialize();

        const parsedUrl = new URL(downloadUrl);
        if (parsedUrl.protocol !== 'https:' && parsedUrl.protocol !== 'http:') {
            throw new Error('Only HTTP and HTTPS download URLs are supported');
        }

        const outputName = options.outputName || path.basename(parsedUrl.pathname) || 'curator-output.jar';
        const safeName = outputName.replace(/[^a-zA-Z0-9._-]/g, '_');
        const destination = path.join(this.inboxDir, `${Date.now()}-${safeName}`);

        await new Promise((resolve, reject) => {
            const client = parsedUrl.protocol === 'https:' ? https : http;
            const request = client.get(parsedUrl, (response) => {
                if (response.statusCode && response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
                    this.receiveJarFromUrl(response.headers.location, options).then(resolve).catch(reject);
                    response.resume();
                    return;
                }

                if (response.statusCode !== 200) {
                    response.resume();
                    reject(new Error(`Download failed with status ${response.statusCode}`));
                    return;
                }

                const file = fsSync.createWriteStream(destination);
                response.pipe(file);

                file.on('finish', () => {
                    file.close(resolve);
                });

                file.on('error', (error) => {
                    file.close(() => reject(error));
                });
            });

            request.on('error', reject);
        });

        const stats = await fs.stat(destination);
        const record = {
            sourcePath: downloadUrl,
            receivedPath: destination,
            receivedAt: new Date().toISOString(),
            size: stats.size
        };

        this.latestJar = record.receivedPath;
        this.receivedJarHistory.unshift(record);
        this.receivedJarHistory = this.receivedJarHistory.slice(0, 10);

        return record;
    }

    getStatus() {
        return {
            bridgeActive: true,
            initialized: this.initialized,
            version: this.version,
            tyncConnected: this.tyncConnection.testConnection(),
            injectionReady: true,
            inboxDir: this.inboxDir,
            latestJar: this.latestJar,
            runningServers: this.serverLauncher.getRunningServers().length
        };
    }

    async shutdown() {
        await this.serverLauncher.stopAllServers();
        this.userInterface.cleanup();
        this.initialized = false;
    }

    async start(argv = process.argv.slice(2)) {
        await this.initialize();

        const jarSource = this.resolveJarArgument(argv);
        if (jarSource) {
            const received = typeof jarSource === 'object' && jarSource.type === 'url'
                ? await this.receiveJarFromUrl(jarSource.value)
                : await this.receiveJar(jarSource);
            console.log(`Received JAR: ${received.receivedPath}`);
        }

        if (process.stdin.isTTY) {
            await this.userInterface.start();
        }
    }

    resolveJarArgument(argv) {
        const receiveIndex = argv.findIndex((arg) => arg === '--receive-jar' || arg === '--jar');
        if (receiveIndex !== -1 && argv[receiveIndex + 1]) {
            return argv[receiveIndex + 1];
        }

        const urlIndex = argv.findIndex((arg) => arg === '--receive-url' || arg === '--url');
        if (urlIndex !== -1 && argv[urlIndex + 1]) {
            return { type: 'url', value: argv[urlIndex + 1] };
        }

        const directJar = argv.find((arg) => typeof arg === 'string' && arg.toLowerCase().endsWith('.jar'));
        if (directJar) {
            return directJar;
        }

        if (process.env.LD_RECEIVE_URL) {
            return { type: 'url', value: process.env.LD_RECEIVE_URL };
        }

        return process.env.LD_RECEIVE_JAR || null;
    }
}

module.exports = LDMinecraftBridge;
module.exports.LDMinecraftBridge = LDMinecraftBridge;

if (require.main === module) {
    const bridge = new LDMinecraftBridge();
    bridge.start().catch((error) => {
        console.error('LD Minecraft Bridge failed to start:', error.message);
        process.exit(1);
    });
}
