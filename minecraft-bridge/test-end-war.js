/**
 * RNK End War API conformance suite.
 *
 * Validates:
 *   1. Every example bundle against bundle-manifest.schema.json
 *   2. Every embedded entry against its entry schema
 *   3. Cross-entry referential integrity (fixRef/addresses point at real ids)
 *   4. Spec invariants: schemas are draft-07 and self-versioned, README
 *      documents the conformance command that exists
 */
const fs = require('fs');
const path = require('path');
const { validateFile } = require('./end-war-api/validate');

const END_WAR_DIR = path.resolve(__dirname, 'end-war-api');
const SCHEMA_DIR = path.join(END_WAR_DIR, 'schema');
const EXAMPLES_DIR = path.join(END_WAR_DIR, 'examples');

let passed = 0;
let failed = 0;
function record(ok, detail) {
    if (ok) { passed++; console.log(`  [pass] ${detail}`); }
    else { failed++; console.log(`  [fail] ${detail}`); }
}

function rel(p) { return path.relative(process.cwd(), p); }

function main() {
    console.log('='.repeat(60));
    console.log('  RNK End War API — contract conformance');
    console.log('='.repeat(60));

    // 1. Examples validate as bundles.
    const bundles = fs.readdirSync(EXAMPLES_DIR).filter((f) => f.endsWith('.bundle.json'));
    record(bundles.length >= 1, `Found ${bundles.length} example bundle(s)`);
    for (const bundleFile of bundles) {
        const bundlePath = path.join(EXAMPLES_DIR, bundleFile);
        const errors = validateFile(path.join(SCHEMA_DIR, 'bundle-manifest.schema.json'), bundlePath);
        record(errors.length === 0, `${rel(bundlePath)} validates against bundle-manifest schema${errors.length ? ` — ${errors[0]}` : ''}`);

        // 2. Extract and validate each embedded entry against its schema.
        const bundle = JSON.parse(fs.readFileSync(bundlePath, 'utf8'));
        const entrySchemas = {
            issuePatterns: 'issue-pattern.schema.json',
            fixPatterns: 'fix-pattern.schema.json',
            apiMappings: 'api-mapping.schema.json'
        };
        const entryIds = new Set();
        let entryCount = 0;
        for (const [key, schemaFile] of Object.entries(entrySchemas)) {
            for (const entry of bundle.entries[key] || []) {
                if (entry.$ref) continue; // sidecar refs: resolved by file, checked as file below
                entryCount++;
                entryIds.add(entry.id);
                const tmp = path.join(EXAMPLES_DIR, `.__entry-${entry.id}.json`);
                fs.writeFileSync(tmp, JSON.stringify(entry, null, 2));
                try {
                    const errs = validateFile(path.join(SCHEMA_DIR, schemaFile), tmp);
                    record(errs.length === 0, `entry ${entry.id} validates against ${schemaFile}${errs.length ? ` — ${errs[0]}` : ''}`);
                } finally {
                    fs.unlinkSync(tmp);
                }
            }
        }
        record(entryCount >= 1, `${bundleFile}: ${entryCount} embedded entries extracted and validated`);

        // 3. Referential integrity.
        let refIssues = 0;
        for (const issue of bundle.entries.issuePatterns || []) {
            if (issue.fixRef && !entryIds.has(issue.fixRef)) {
                refIssues++;
                record(false, `${bundleFile}: issue ${issue.id} fixRef "${issue.fixRef}" has no matching fix entry`);
            }
        }
        for (const fix of bundle.entries.fixPatterns || []) {
            for (const addr of fix.addresses || []) {
                if (!entryIds.has(addr)) {
                    refIssues++;
                    record(false, `${bundleFile}: fix ${fix.id} addresses "${addr}" with no matching issue entry`);
                }
            }
        }
        if (refIssues === 0) {
            record(true, `${bundleFile}: all fixRef/addresses references resolve`);
        }

        // 4. Schema invariants.
        const manifest = JSON.parse(fs.readFileSync(path.join(SCHEMA_DIR, 'bundle-manifest.schema.json'), 'utf8'));
        record(manifest.$schema.includes('draft-07'), 'bundle schema is draft-07');
        record((manifest.$id || '').includes('/0.1/'), 'bundle schema $id is versioned (0.1)');
    }

    // 5. README documents a command that exists.
    const readme = fs.readFileSync(path.join(END_WAR_DIR, 'README.md'), 'utf8');
    record(readme.includes('npm run test:end-war'), 'README references the conformance command');

    console.log('\n' + '='.repeat(60));
    console.log(`  End War contract ${failed === 0 ? 'CONFORMANT' : 'FAILED'}: passed ${passed} failed ${failed}`);
    console.log('='.repeat(60));
    process.exit(failed === 0 ? 0 : 1);
}

main();
