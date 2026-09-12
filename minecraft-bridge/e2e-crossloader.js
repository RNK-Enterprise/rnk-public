/**
 * RNK Minecraft Bridge — Cross-Loader Adaptation E2E
 *
 * Proves the REAL Fabric → Paper transformation end-to-end through the
 * Bridge pipeline, for BOTH entrypoint paths. No mocks in the
 * transformation path:
 *
 *   1. Build both demo Fabric mods (heartsync) from source fixtures
 *        - server: DedicatedServerModInitializer entrypoint
 *        - client: ModInitializer (main) entrypoint
 *   2. Receive each into the Bridge inbox
 *   3. CrossLoaderAdapterEngine (real JVM): ASM rewrite Fabric → Paper
 *   4. CrossLoaderAdapterEngine verify: execute each adapted artifact in an
 *      isolated classloader — the mod's actual onInitialize() must run, and
 *      each initializer must have taken ITS path's shim
 *   5. Deliver: copy adapted artifacts + manifest into the test server
 *
 * Run: npm run test:crossloader
 */
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const RNKMinecraftBridge = require('./index');

const FIXTURE_DIR = path.resolve(__dirname, 'test-fixtures');
const BUILD_SCRIPT = path.join(FIXTURE_DIR, 'build-fabric-fixture.sh');
const SERVER_DIR = path.resolve(__dirname, 'test-servers', 'test-server-1');
const MODS_DIR = path.join(SERVER_DIR, 'mods');
const WORK_DIR = path.resolve(__dirname, 'test-servers', 'crossloader-demo');

// The two entrypoint paths under test: fixture JAR name -> expected shim.
const FIXTURES = [
    { name: 'server', jar: 'heartsync-fabric.jar', expectedShim: 'com.rnk.shim.DedicatedServerModInitializer' },
    { name: 'client', jar: 'heartsync-client-fabric.jar', expectedShim: 'com.rnk.shim.ModInitializer' }
];

let passed = 0;
let failed = 0;

function step(name) { console.log(`\n▶ ${name}`); }
function record(ok, detail) {
    if (ok) { passed++; console.log(`  ✅ ${detail}`); }
    else { failed++; console.log(`  ❌ ${detail}`); }
}

async function main() {
    console.log('='.repeat(60));
    console.log('  RNK Cross-Loader Adaptation — Fabric mods → Paper artifacts (REAL)');
    console.log('='.repeat(60));

    const bridge = new RNKMinecraftBridge();
    await bridge.initialize();

    step('1. Build both Fabric fixture mods from source');
    execSync(`bash "${BUILD_SCRIPT}" "${WORK_DIR}"`, {
        stdio: 'inherit',
        env: { ...process.env, BUILD_ONLY: '1' }
    });
    for (const fixture of FIXTURES) {
        const jarPath = path.join(WORK_DIR, fixture.jar);
        record(fs.existsSync(jarPath) && fs.statSync(jarPath).size > 500,
            `[${fixture.name}] Fabric mod JAR built (${fs.statSync(jarPath).size}B)`);
    }

    const results = [];

    for (const fixture of FIXTURES) {
        console.log(`\n━━━ Fixture: ${fixture.name} (expects shim ${fixture.expectedShim})`);

        step(`2. [${fixture.name}] Receive mod into Bridge inbox`);
        const fabricJar = path.join(WORK_DIR, fixture.jar);
        const received = await bridge.receiveJar(fabricJar);
        record(fs.existsSync(received.receivedPath), `In inbox: ${path.basename(received.receivedPath)}`);
        record(bridge.latestJar === received.receivedPath, 'latestJar points at the new mod');

        step(`3. [${fixture.name}] CrossLoaderAdapterEngine — adapt (real ASM rewrite)`);
        const adaptedJar = path.join(WORK_DIR, fixture.jar.replace('.jar', '-adapted.jar'));
        const adapted = await bridge.tyncConnection.callTyncEngine('CrossLoaderAdapterEngine', {
            requestType: 'adapt',
            modPath: received.receivedPath,
            outputPath: adaptedJar
        });
        record(adapted.success === true, `Engine call succeeded (${adapted.executionTimeMs}ms)`);
        record(adapted.adaptation.sourceLoader === 'fabric' && adapted.adaptation.targetLoader === 'paper',
            'Source loader: fabric → target loader: paper');
        record(Array.isArray(adapted.entrypoints) && adapted.entrypoints.length === 1,
            `Entrypoint detected: ${adapted.entrypoints.join(', ')}`);
        record(adapted.classesRewritten.length === 1,
            `Mod class rewritten through ASM remapper: ${adapted.classesRewritten.join(', ')}`);
        record(fs.existsSync(adapted.adaptedJar), `Adapted artifact written: ${path.basename(adapted.adaptedJar)}`);

        step(`4. [${fixture.name}] CrossLoaderAdapterEngine — verify (execute artifact)`);
        const verification = await bridge.tyncConnection.callTyncEngine('CrossLoaderAdapterEngine', {
            requestType: 'verify',
            adaptedJarPath: adapted.adaptedJar
        });
        record(verification.success === true && verification.verified === true,
            `Adapted artifact EXECUTED (${verification.executionTimeMs}ms)`);
        record(verification.initializerExecuted === true, 'Mod\'s real onInitialize() ran in the adapted artifact');
        record(verification.initializerImplementsShim === true, 'Initializer implements an RNK shim (not Fabric API)');
        record(verification.shimImplemented === fixture.expectedShim,
            `Took the expected remap path → ${verification.shimImplemented}`);
        record(verification.fabricInterfaceAbsent === true, 'Fabric interface absent from the adapted classpath');

        results.push({ fixture, adapted, verification, received });
    }

    step('5. Deliver adapted artifacts into test server');
    fs.mkdirSync(MODS_DIR, { recursive: true });
    const manifests = [];
    for (const { fixture, adapted, verification, received } of results) {
        const deliveredPath = path.join(MODS_DIR, path.basename(adapted.adaptedJar));
        fs.copyFileSync(adapted.adaptedJar, deliveredPath);
        record(fs.existsSync(deliveredPath),
            `[${fixture.name}] Installed: mods/${path.basename(deliveredPath)} (${fs.statSync(deliveredPath).size}B)`);
        manifests.push({
            sourceLoader: 'fabric',
            targetLoader: 'paper',
            fixture: fixture.name,
            sourceJar: received.receivedPath,
            adaptedJar: deliveredPath,
            entrypoints: adapted.entrypoints,
            classesRewritten: adapted.classesRewritten,
            executionProof: {
                initializerExecuted: verification.initializerExecuted,
                shimImplemented: verification.shimImplemented,
                verified: verification.verified
            }
        });
    }
    fs.writeFileSync(path.join(SERVER_DIR, 'crossloader-manifest.json'), JSON.stringify(manifests, null, 2));
    record(fs.existsSync(path.join(SERVER_DIR, 'crossloader-manifest.json')), 'Cross-loader manifest written (both fixtures)');

    console.log('\n' + '='.repeat(60));
    console.log(`  Cross-loader adaptation ${failed === 0 ? 'PROVEN' : 'FAILED'}: ✅ ${passed}  ❌ ${failed}`);
    console.log('='.repeat(60));
    process.exit(failed === 0 ? 0 : 1);
}

main().catch((error) => {
    console.error('Cross-loader E2E crashed:', error);
    process.exit(1);
});
