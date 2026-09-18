/**
 * RNK End War API — dependency-free conformance checker.
 *
 * Validates JSON instances against the contract's draft-07 schemas, covering
 * the schema subset the End War contract uses:
 *   type (object/array/string), required, properties, additionalProperties,
 *   enum, pattern, items, minItems, minLength, $ref (local definitions and
 *   sibling-schema files), and oneOf (structural, reports if none match).
 *
 * The contract's schemas are deliberately kept inside this subset so this
 * checker is complete FOR THEM; production implementers may substitute a
 * full draft-07 validator (e.g. ajv).
 *
 * Usage: node end-war-api/validate.js <schema.json> <instance.json> [...]
 * Exit 0 iff every instance validates.
 */
const fs = require('fs');
const path = require('path');

function resolveRef(rootDir, rootSchema, ref) {
    if (ref.startsWith('#/')) {
        const segs = ref.slice(2).split('/');
        let node = rootSchema;
        for (const s of segs) node = node[s];
        return { schema: node, rootDir };
    }
    // File ref relative to the current schema file's directory.
    const file = path.resolve(rootDir, ref);
    const schema = JSON.parse(fs.readFileSync(file, 'utf8'));
    return { schema, rootDir: path.dirname(file) };
}

function fail(instancePath, message) {
    const err = new Error(`${instancePath || '(root)'}: ${message}`);
    err.rnkValidation = true;
    throw err;
}

function validate(schema, instance, instancePath, rootDir, rootSchema, errors) {
    if (schema.$ref) {
        const { schema: resolved, rootDir: dir } = resolveRef(rootDir, rootSchema, schema.$ref);
        validate(resolved, instance, instancePath, dir, resolved, errors);
        return;
    }
    if (schema.oneOf) {
        const attempts = errors.count;
        let matches = 0;
        for (const sub of schema.oneOf) {
            errors.count = attempts;
            try {
                validate(sub, instance, instancePath, rootDir, rootSchema, errors);
                matches++;
            } catch (e) {
                if (!e.rnkValidation) throw e;
            }
        }
        errors.count = attempts;
        if (matches !== 1) {
            fail(instancePath, `expected exactly one oneOf match, got ${matches}`);
        }
        return;
    }
    if (schema.type) {
        const t = schema.type;
        const actual = Array.isArray(instance) ? 'array'
            : instance === null ? 'null'
            : typeof instance === 'number' ? 'number'
            : typeof instance === 'boolean' ? 'boolean'
            : typeof instance === 'string' ? 'string'
            : typeof instance === 'object' ? 'object' : 'unknown';
        const ok = t === 'integer' ? Number.isInteger(instance) : t === actual;
        if (!ok) {
            fail(instancePath, `expected ${t}, got ${actual}`);
        }
    }
    if (typeof instance === 'string') {
        if (schema.minLength !== undefined && instance.length < schema.minLength) {
            fail(instancePath, `shorter than minLength ${schema.minLength}`);
        }
        if (schema.pattern !== undefined && !new RegExp(schema.pattern).test(instance)) {
            fail(instancePath, `does not match pattern ${schema.pattern}`);
        }
        if (schema.enum !== undefined && !schema.enum.includes(instance)) {
            fail(instancePath, `not in enum [${schema.enum.join(', ')}]`);
        }
    }
    if (Array.isArray(instance)) {
        if (schema.minItems !== undefined && instance.length < schema.minItems) {
            fail(instancePath, `fewer than minItems ${schema.minItems}`);
        }
        if (schema.items) {
            instance.forEach((item, i) => {
                validate(schema.items, item, `${instancePath}[${i}]`, rootDir, rootSchema, errors);
            });
        }
    }
    if (instance !== null && typeof instance === 'object' && !Array.isArray(instance)) {
        for (const req of schema.required || []) {
            if (!(req in instance)) {
                fail(instancePath, `missing required property "${req}"`);
            }
        }
        const props = schema.properties || {};
        for (const [key, value] of Object.entries(instance)) {
            if (props[key]) {
                validate(props[key], value, `${instancePath}.${key}`, rootDir, rootSchema, errors);
            } else if (schema.additionalProperties === false) {
                fail(`${instancePath}.${key}`, 'additional property not allowed');
            }
        }
    }
}

function validateFile(schemaPath, instancePath) {
    const rootDir = path.dirname(path.resolve(schemaPath));
    const rootSchema = JSON.parse(fs.readFileSync(schemaPath, 'utf8'));
    const instance = JSON.parse(fs.readFileSync(instancePath, 'utf8'));
    const errors = { count: 0 };
    const collected = [];
    try {
        validate(rootSchema, instance, '', rootDir, rootSchema, errors);
    } catch (e) {
        if (!e.rnkValidation) throw e;
        collected.push(e.message);
    }
    return collected;
}

function main() {
    const args = process.argv.slice(2);
    if (args.length < 2 || args.length % 2 !== 0) {
        console.error('Usage: node end-war-api/validate.js <schema.json> <instance.json> [...]');
        process.exit(2);
    }
    let failures = 0;
    for (let i = 0; i < args.length; i += 2) {
        const [schemaPath, instancePath] = [args[i], args[i + 1]];
        const errors = validateFile(schemaPath, instancePath);
        if (errors.length === 0) {
            console.log(`  [pass] ${path.relative(process.cwd(), instancePath)} conforms to ${path.basename(schemaPath)}`);
        } else {
            failures++;
            console.log(`  [fail] ${path.relative(process.cwd(), instancePath)}:`);
            for (const e of errors) console.log(`      ${e}`);
        }
    }
    console.log(failures === 0
        ? '\nAll instances conform.'
        : `\n${failures} instance(s) failed conformance.`);
    process.exit(failures === 0 ? 0 : 1);
}

if (require.main === module) {
    main();
}

module.exports = { validateFile };
