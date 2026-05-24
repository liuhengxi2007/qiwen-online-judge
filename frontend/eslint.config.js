import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

const dependencyHookNames = new Set([
  'useCallback',
  'useEffect',
  'useImperativeHandle',
  'useInsertionEffect',
  'useLayoutEffect',
  'useMemo',
])

function getCalleeName(callee) {
  if (callee.type === 'Identifier') {
    return callee.name
  }

  if (callee.type === 'MemberExpression' && !callee.computed && callee.property.type === 'Identifier') {
    return callee.property.name
  }

  return null
}

const localRules = {
  rules: {
    'max-hook-dependencies': {
      meta: {
        type: 'problem',
        docs: {
          description: 'Limit React hook dependency arrays to a small number of dependencies.',
        },
        schema: [
          {
            type: 'object',
            properties: {
              max: {
                type: 'integer',
                minimum: 0,
              },
            },
            additionalProperties: false,
          },
        ],
        messages: {
          tooManyDependencies: 'Hook dependency array has {{count}} items. Keep dependency arrays to {{max}} items or fewer.',
        },
      },
      create(context) {
        const max = context.options[0]?.max ?? 4

        return {
          CallExpression(node) {
            const hookName = getCalleeName(node.callee)
            if (!hookName || !dependencyHookNames.has(hookName)) {
              return
            }

            const dependencyArrayIndex = hookName === 'useImperativeHandle' ? 2 : 1
            const dependencyArray = node.arguments[dependencyArrayIndex]
            if (!dependencyArray || dependencyArray.type !== 'ArrayExpression') {
              return
            }

            const dependencyCount = dependencyArray.elements.length
            if (dependencyCount <= max) {
              return
            }

            context.report({
              node: dependencyArray,
              messageId: 'tooManyDependencies',
              data: {
                count: String(dependencyCount),
                max: String(max),
              },
            })
          },
        }
      },
    },
  },
}

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      local: localRules,
    },
    rules: {
      'react-hooks/exhaustive-deps': 'error',
      'local/max-hook-dependencies': ['error', { max: 4 }],
      'react-refresh/only-export-components': [
        'error',
        { allowConstantExport: true },
      ],
    },
  },
  {
    files: ['src/components/ui/**/*.tsx'],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
])
