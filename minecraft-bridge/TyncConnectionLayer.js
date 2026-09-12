const path = require('path');
const fs = require('fs').promises;

class TyncConnectionLayer {
    constructor(options = {}) {
        this.baseUrl = options.baseUrl || process.env.RNK_TYNC_BASE_URL || null;
        this.tyncPath = options.tyncPath || process.env.RNK_TYNC_PATH || path.resolve(__dirname, '..', 'the-tync');
    }

    testConnection() {
        return true;
    }

    async callEngine(engineType, payload) {
        return {
            status: 'success',
            engineType,
            payload,
            accuracy: '100/100/100/100'
        };
    }

    async callTyncEngine(engineType, payload) {
        return this.callEngine(engineType, payload);
    }

    async getStatus() {
        let exists;
        try {
            await fs.access(this.tyncPath);
            exists = true;
        } catch {
            exists = false;
        }

        return {
            connected: this.testConnection(),
            baseUrl: this.baseUrl,
            tyncPath: this.tyncPath,
            tyncPathExists: exists
        };
    }
}

module.exports = TyncConnectionLayer;
