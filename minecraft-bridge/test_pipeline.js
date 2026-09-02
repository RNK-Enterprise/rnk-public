const LDMinecraftBridge = require('./index.js');

async function testFullPipeline() {
    const bridge = new LDMinecraftBridge();

    try {
        await bridge.initialize();
        console.log('Bridge initialized successfully');

        // Get detected servers
        const servers = await bridge.serverLauncher.detectServers();
        console.log(`Found ${servers.length} server(s)`);

        if (servers.length > 0) {
            const server = servers[0];
            console.log(`Testing with server: ${server.name} at ${server.path}`);

            // Launch server with mods
            await bridge.userInterface.launchServerWithMods(server);
        } else {
            console.log('No servers found');
        }
    } catch (error) {
        console.error('Test failed:', error.message);
    }
}

testFullPipeline();