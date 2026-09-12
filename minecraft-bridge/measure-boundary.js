/**
 * RNK Boundary Measurement — quantify the §4 supported scope on REAL mods.
 *
 * Samples the Modrinth population matching the paper's declared scope
 * (server-side-required, client-unsupported, Fabric, 1.20.1), downloads each
 * mod's primary JAR, and runs it through the real adapter + verifier:
 *
 *   classify    → in-boundary (adapter reports success AND the adapted
 *                artifact passes structural verification: manifest markers,
 *                HMAC provenance, required entries, shim shape, dispatch
 *                method — checked on bytecode, no classloading)
 *              → out-of-boundary (adapter rejects or structure fails) with
 *                the adapter's own reason
 *
 * Self-contained execution (the stub-classloader harness) is deliberately
 * NOT part of the boundary verdict: real mods reference Minecraft/Fabric
 * classes no isolated harness can resolve. Execution proof is demonstrated
 * on the self-contained fixtures (npm run test:crossloader, real Paper via
 * npm run verify:paper); this measurement classifies structure only.
 *
 * Output: an on-disk JSON report (boundary-measurement.json) with the exact
 * numbers the whitepaper's §4 needs, plus a human-readable summary.
 *
 * Usage:
 *   node measure-boundary.js [sampleSize]     (default 50)
 *
 * Notes:
 *   - Modrinth API is rate-limited (~300 req/min); the script self-paces.
 *   - Classification is conservative: anything the adapter does not accept
 *     with success:true counts as out-of-boundary.
 */
const fs = require('fs');
const path = require('path');

const RNKMinecraftBridge = require('./index');

const SAMPLE_SIZE = parseInt(process.argv[2] || '50', 10);
const WORK_DIR = path.resolve(__dirname, 'test-servers', 'boundary-measurement');
const DOWNLOAD_DIR = path.join(WORK_DIR, 'jars');
const REPORT_PATH = path.resolve(__dirname, 'boundary-measurement.json');

const SEARCH_URL =
    'https://api.modrinth.com/v2/search?limit=100&index=downloads' +
    '&facets=%5B%5B%22project_type:mod%22%5D,%5B%22server_side:required%22%5D,' +
    '%5B%22client_side:unsupported%22%5D,%5B%22versions:1.20.1%22%5D,' +
    '%5B%22loaders:fabric%22%5D%5D';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function fetchJson(url) {
    const res = await fetch(url, { headers: { 'User-Agent': 'RNK-boundary-measurement/1.0' } });
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return res.json();
}

async function main() {
    console.log('='.repeat(64));
    console.log(`  RNK boundary measurement — ${SAMPLE_SIZE} real Fabric 1.20.1 server-side mods`);
    console.log('='.repeat(64));

    fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });

    // 1. Discover the population, top-N by downloads.
    console.log('\n▶ Querying Modrinth population…');
    const search = await fetchJson(SEARCH_URL);
    const population = search.total_hits;
    const candidates = search.hits.slice(0, SAMPLE_SIZE);
    console.log(`  Population matching scope facets: ${population} mods`);
    console.log(`  Sampling top ${candidates.length} by downloads:`);
    for (const h of candidates) console.log(`    · ${h.slug} (${h.downloads.toLocaleString()} dl)`);

    const bridge = new RNKMinecraftBridge();
    await bridge.initialize();

    const rows = [];
    let inBoundary = 0;

    for (const hit of candidates) {
        const row = {
            slug: hit.slug,
            downloads: hit.downloads,
            status: 'error',
            stage: null,
            reason: null,
            verified: null
        };
        try {
            // 2. Download the primary version file (first version matching 1.20.1+fabric).
            const versions = await fetchJson(`https://api.modrinth.com/v2/project/${hit.slug}/version`);
            await sleep(350); // stay well under the API rate limit
            const v = versions.find((x) => x.game_versions.includes('1.20.1') && x.loaders.includes('fabric'));
            if (!v) {
                row.status = 'out';
                row.stage = 'download';
                row.reason = 'no 1.20.1 fabric file in version list';
                rows.push(row);
                console.log(`  ⊘ ${hit.slug}: no 1.20.1 fabric file`);
                continue;
            }
            const file = v.files.find((f) => f.primary) || v.files[0];
            const dest = path.join(DOWNLOAD_DIR, `${hit.slug}.jar`);
            const jarRes = await fetch(file.url, { headers: { 'User-Agent': 'RNK-boundary-measurement/1.0' } });
            if (!jarRes.ok) throw new Error(`download HTTP ${jarRes.status}`);
            fs.writeFileSync(dest, Buffer.from(await jarRes.arrayBuffer()));
            await sleep(350);

            // 3. Adapt with the real engine.
            const outJar = path.join(WORK_DIR, `${hit.slug}-adapted.jar`);
            const adapted = await bridge.tyncConnection.callTyncEngine('CrossLoaderAdapterEngine', {
                requestType: 'adapt',
                modPath: dest,
                outputPath: outJar
            });
            if (adapted.success !== true) {
                row.status = 'out';
                row.stage = 'adapt';
                row.reason = adapted.error || 'engine reported failure';
                rows.push(row);
                console.log(`  ✗ ${hit.slug}: OUT — ${row.reason}`);
                continue;
            }

            // 4. Structural verification of the adapted artifact (no
            //    classloading — valid for real mods with unresolvable deps).
            const verification = await bridge.tyncConnection.callTyncEngine('CrossLoaderAdapterEngine', {
                requestType: 'verify_structure',
                adaptedJarPath: adapted.adaptedJar
            });
            if (verification.success !== true || verification.structureVerified !== true) {
                row.status = 'out';
                row.stage = 'verify';
                row.reason = verification.error || 'structural verification failed';
                rows.push(row);
                console.log(`  ✗ ${hit.slug}: OUT — ${row.reason}`);
                continue;
            }
            row.status = 'in';
            row.stage = 'verify';
            row.verified = true;
            row.shim = verification.shimImplemented;
            inBoundary++;
            console.log(`  ✓ ${hit.slug}: IN (shim ${verification.shimImplemented})`);
        } catch (err) {
            row.status = 'out';
            row.reason = String(err.message || err).slice(0, 300);
            console.log(`  ✗ ${hit.slug}: OUT — ${row.reason}`);
        }
        rows.push(row);
    }

    const n = rows.length;
    const summary = {
        measuredAt: new Date().toISOString(),
        population: {
            source: 'Modrinth API v2',
            facets: ['project_type:mod', 'server_side:required', 'client_side:unsupported', 'versions:1.20.1', 'loaders:fabric'],
            totalMatching: population,
            sampled: n,
            samplingRule: `top ${SAMPLE_SIZE} by downloads (descending)`
        },
        result: {
            inBoundary,
            outOfBoundary: n - inBoundary,
            passRate: n ? +(inBoundary / n).toFixed(3) : null,
            note: 'boundary verdict = adapt success + structural verification; execution proof is fixture-suite capability (see README)'
        },
        rows
    };

    fs.writeFileSync(REPORT_PATH, JSON.stringify(summary, null, 2));

    console.log('\n' + '='.repeat(64));
    console.log(`  RESULT: ${inBoundary}/${n} sampled mods inside the supported boundary`);
    if (n) {
        console.log(`  Pass rate: ${Math.round((inBoundary / n) * 100)}%`);
    }
    console.log(`  Report: ${REPORT_PATH}`);
    console.log('='.repeat(64));
    process.exit(0);
}

main().catch((err) => {
    console.error('measurement failed:', err);
    process.exit(1);
});
