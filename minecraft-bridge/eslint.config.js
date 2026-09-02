const js = require('@eslint/js');

module.exports = [
    js.configs.recommended,
    {
        languageOptions: {
            ecmaVersion: 'latest',
            sourceType: 'commonjs',
            globals: {
                console: 'readonly',
                process: 'readonly',
                global: 'readonly',
                require: 'readonly',
                module: 'readonly',
                __dirname: 'readonly',
                __filename: 'readonly',
                Buffer: 'readonly',
                setTimeout: 'readonly',
                clearTimeout: 'readonly'
            }
        },
        rules: {
            indent: ['off'], // Disable for config file
            'linebreak-style': ['off'], // Allow both LF and CRLF
            quotes: ['error', 'single', { allowTemplateLiterals: true }],
            semi: ['error', 'always'],
            'no-unused-vars': ['error', { argsIgnorePattern: '^_', caughtErrors: 'none' }],
            'preserve-caught-error': ['off'] // Disable for now
        }
    }
];