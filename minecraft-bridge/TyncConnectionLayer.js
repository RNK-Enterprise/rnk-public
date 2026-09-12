const { spawn } = require('child_process');
const http = require('http');
const https = require('https');
const { URL } = require('url');
const path = require('path');
const fs = require('fs');
const fsp = require('fs').promises;

// JVM startup dominates engine call time; give the child process a generous ceiling.
const ENGINE_CALL_TIMEOUT_MS = 60_000;

class TyncConnectionLayer {
    constructor(options = {}) {
        this.baseUrl = options.baseUrl || process.env.RNK_TYNC_BASE_URL || null;
        this.authToken = options.authToken || process.env.RNK_TYNC_AUTH_TOKEN || null;
        this.mode = this.baseUrl ? 'http' : 'local';
        this.tyncPath = options.tyncPath || process.env.RNK_TYNC_PATH || path.resolve(__dirname, '..', 'the-tync');

        this.javaPath = options.javaPath || process.env.TYNC_JAVA_PATH || this.resolveJavaPath();
        this.classpath = options.classpath || process.env.TYNC_CLASSPATH || this.resolveClasspath();
        this.runnerClass = options.runnerClass || process.env.TYNC_RUNNER || 'com.rnk.thetync.SimpleEngineRunner';
        this.timeoutMs = options.timeoutMs || Number(process.env.TYNC_CALL_TIMEOUT_MS) || ENGINE_CALL_TIMEOUT_MS;
    }

    /**
     * Resolve the java executable: bundled Tync JDK first, then JAVA_HOME, then PATH.
     */
    resolveJavaPath() {
        const exe = process.platform === 'win32' ? 'java.exe' : 'java';
        const candidates = [
            path.join(this.tyncPath, 'jdk21', 'jdk-21.0.2+13', 'bin', exe),
            process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', exe) : null
        ].filter(Boolean);

        for (const candidate of candidates) {
            try {
                fs.accessSync(candidate, fs.constants.X_OK);
                return candidate;
            } catch {
                // fall through to plain `java` on PATH
            }
        }
        return 'java';
    }

    /** Path to the compiled Tync application JAR produced by the Maven build. */
    getJarPath() {
        return path.join(this.tyncPath, 'main-app', 'target', 'the-tync-main-1.0.0.jar');
    }

    /** Directory of runtime dependency JARs copied by the Maven build. */
    getDependencyDir() {
        return path.join(this.tyncPath, 'main-app', 'target', 'dependency');
    }

    /**
     * Build the classpath for the compiled Tync application JAR plus its dependency JARs.
     */
    resolveClasspath() {
        return `${this.getJarPath()}${path.delimiter}${path.join(this.getDependencyDir(), '*')}`;
    }

    /**
     * Fail-fast check that the standard Maven build outputs exist and are usable:
     * the application JAR is present and the dependency directory holds JARs
     * (Jackson included — SimpleEngineRunner links against it). Throws with
     * remediation instructions when the build is missing, stale, or incomplete.
     */
    verifyBuildArtifacts() {
        const jarPath = this.getJarPath();
        const dependencyDir = this.getDependencyDir();
        const problems = [];

        if (!fs.existsSync(jarPath)) {
            problems.push(`application JAR not found: ${jarPath}`);
        }

        const dependencyJars = fs.existsSync(dependencyDir)
            ? fs.readdirSync(dependencyDir).filter((file) => file.endsWith('.jar'))
            : [];
        if (dependencyJars.length === 0) {
            problems.push(`dependency directory missing or empty: ${dependencyDir}`);
        } else if (!dependencyJars.some((file) => file.startsWith('jackson-databind-'))) {
            problems.push(`jackson-databind JAR not found in ${dependencyDir} (required by ${this.runnerClass})`);
        }

        if (problems.length > 0) {
            const exe = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';
            const bundledMaven = path.join(this.tyncPath, 'apache-maven-3.9.9', 'bin', exe);
            const mvnCommand = fs.existsSync(bundledMaven) ? bundledMaven : 'mvn';
            throw new Error(
                'Tync Maven build artifacts missing or incomplete.\n' +
                problems.map((problem) => `  - ${problem}`).join('\n') + '\n' +
                `Build them first:\n  cd "${this.tyncPath}" && ${mvnCommand} clean package`
            );
        }

        return { jarPath, dependencyDir, dependencyCount: dependencyJars.length };
    }

    testConnection() {
        if (this.mode === 'http') {
            return Boolean(this.baseUrl);
        }
        return Boolean(this.javaPath) && Boolean(this.classpath);
    }

    /**
     * Execute an engine: over HTTP against the configured hosted endpoint,
     * or by spawning the local SimpleEngineRunner JVM process.
     */
    async callEngine(engineType, payload = {}) {
        if (this.mode === 'http') {
            return this.callEngineHttp(engineType, payload);
        }
        return this.callEngineLocal(engineType, payload);
    }

    /**
     * Hosted endpoint transport: POST <baseUrl>/engine/<engineName> with the
     * JSON payload and (when configured) a Bearer token. Same JSON contract
     * and error semantics as the local transport.
     *
     * Security default: plaintext HTTP is refused for non-loopback hosts
     * (tokens must not travel unencrypted); override with
     * RNK_ALLOW_INSECURE_TYNC=1 for trusted private networks.
     */
    async callEngineHttp(engineType, payload = {}) {
        const body = JSON.stringify(payload);
        const endpoint = new URL(`${this.baseUrl.replace(/\/+$/, '')}/engine/${encodeURIComponent(engineType)}`);
        const isLoopback = ['localhost', '127.0.0.1', '::1'].includes(endpoint.hostname);
        if (endpoint.protocol === 'http:' && !isLoopback && process.env.RNK_ALLOW_INSECURE_TYNC !== '1') {
            throw new Error(`Refusing plaintext HTTP to non-local Tync endpoint '${endpoint.hostname}'. Use HTTPS, or set RNK_ALLOW_INSECURE_TYNC=1 for a trusted private network.`);
        }
        const transport = endpoint.protocol === 'https:' ? https : http;

        return new Promise((resolve, reject) => {
            const startedAt = Date.now();
            let settled = false;

            const request = transport.request(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'X-RNK-Client': 'rnk-minecraft-bridge',
                    ...(this.authToken ? { Authorization: `Bearer ${this.authToken}` } : {})
                },
                timeout: this.timeoutMs
            }, (response) => {
                let responseBody = '';
                response.setEncoding('utf8');
                response.on('data', (chunk) => { responseBody += chunk; });
                response.on('end', () => {
                    if (settled) return;
                    settled = true;
                    const elapsed = Date.now() - startedAt;

                    if (response.statusCode < 200 || response.statusCode >= 300) {
                        reject(new Error(`Tync engine '${engineType}' HTTP call failed with status ${response.statusCode}: ${responseBody.slice(0, 300) || 'no body'}`));
                        return;
                    }

                    let result;
                    try {
                        result = JSON.parse(responseBody);
                    } catch (error) {
                        reject(new Error(`Failed to parse Tync engine HTTP result for '${engineType}': ${error.message}. Raw: ${responseBody.slice(0, 500)}`));
                        return;
                    }

                    if (result && result.success === false) {
                        const engineError = new Error(result.error || `Tync engine '${engineType}' reported failure`);
                        engineError.code = 'TYNC_ENGINE_REPORTED_FAILURE';
                        reject(engineError);
                        return;
                    }

                    resolve({
                        ...result,
                        engineType,
                        executionTimeMs: elapsed
                    });
                });
            });

            request.on('timeout', () => {
                if (settled) return;
                settled = true;
                request.destroy();
                reject(new Error(`Tync engine HTTP call timed out after ${this.timeoutMs}ms: ${engineType}`));
            });

            request.on('error', (error) => {
                if (settled) return;
                settled = true;
                reject(new Error(`Tync engine HTTP call failed: ${error.message}`));
            });

            request.end(body);
        });
    }

    /**
     * Local transport: spawn SimpleEngineRunner <engineName> <jsonContext>
     * and parse the single-line JSON result from stdout.
     */
    async callEngineLocal(engineType, payload = {}) {
        const jsonContext = JSON.stringify(payload);

        return new Promise((resolve, reject) => {
            const startedAt = Date.now();
            const java = spawn(this.javaPath, [
                '-cp', this.classpath,
                this.runnerClass,
                engineType,
                jsonContext
            ], {
                stdio: ['ignore', 'pipe', 'pipe'],
                windowsHide: true
            });

            let stdout = '';
            let stderr = '';
            let settled = false;

            const timeout = setTimeout(() => {
                if (settled) return;
                settled = true;
                java.kill('SIGKILL');
                reject(new Error(`Tync engine call timed out after ${this.timeoutMs}ms: ${engineType}`));
            }, this.timeoutMs);

            java.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
            java.stderr.on('data', (chunk) => { stderr += chunk.toString(); });

            java.on('error', (error) => {
                if (settled) return;
                settled = true;
                clearTimeout(timeout);
                reject(new Error(`Failed to start Tync engine process: ${error.message}`));
            });

            java.on('close', (code) => {
                if (settled) return;
                settled = true;
                clearTimeout(timeout);

                const elapsed = Date.now() - startedAt;
                const output = stdout.trim();

                if (code !== 0) {
                    const error = new Error(`Tync engine '${engineType}' exited with code ${code}: ${stderr.trim() || output || 'no output'}`);
                    error.code = 'TYNC_INFRA_FAILURE';
                    reject(error);
                    return;
                }

                if (!output) {
                    const error = new Error(`Tync engine '${engineType}' produced no output`);
                    error.code = 'TYNC_INFRA_FAILURE';
                    reject(error);
                    return;
                }

                // The runner prints exactly one JSON line; take the last non-empty line for safety.
                const lines = output.split(/\r?\n/).filter((line) => line.trim().length > 0);
                const lastLine = lines[lines.length - 1];

                let result;
                try {
                    result = JSON.parse(lastLine);
                } catch (error) {
                    const parseError = new Error(`Failed to parse Tync engine result for '${engineType}': ${error.message}. Raw: ${output.slice(0, 500)}`);
                    parseError.code = 'TYNC_INFRA_FAILURE';
                    reject(parseError);
                    return;
                }

                if (result && result.success === false) {
                    const engineError = new Error(result.error || `Tync engine '${engineType}' reported failure`);
                    engineError.code = 'TYNC_ENGINE_REPORTED_FAILURE';
                    reject(engineError);
                    return;
                }

                resolve({
                    ...result,
                    engineType,
                    executionTimeMs: elapsed
                });
            });
        });
    }

    async callTyncEngine(engineType, payload) {
        return this.callEngine(engineType, payload);
    }

    async getStatus() {
        let exists = false;
        try {
            await fsp.access(this.tyncPath);
            exists = true;
        } catch {
            // tyncPath not accessible
        }

        return {
            connected: this.testConnection(),
            mode: this.mode,
            baseUrl: this.baseUrl,
            authTokenConfigured: Boolean(this.authToken),
            tyncPath: this.tyncPath,
            tyncPathExists: exists,
            javaPath: this.javaPath,
            classpath: this.classpath,
            runnerClass: this.runnerClass
        };
    }
}

module.exports = TyncConnectionLayer;
