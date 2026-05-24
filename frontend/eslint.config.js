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

function getHookCallbackIndex(hookName) {
  return hookName === 'useImperativeHandle' ? 1 : 0
}

function getHookDependencyArrayIndex(hookName) {
  return hookName === 'useImperativeHandle' ? 2 : 1
}

function unwrapExpression(node) {
  if (
    node.type === 'ChainExpression' ||
    node.type === 'TSAsExpression' ||
    node.type === 'TSNonNullExpression' ||
    node.type === 'TSSatisfiesExpression' ||
    node.type === 'TSInstantiationExpression'
  ) {
    return unwrapExpression(node.expression)
  }

  return node
}

function getStaticDependencyPath(node) {
  const unwrappedNode = unwrapExpression(node)

  if (unwrappedNode.type === 'Identifier') {
    return unwrappedNode.name
  }

  if (
    unwrappedNode.type === 'MemberExpression' &&
    !unwrappedNode.computed &&
    unwrappedNode.property.type === 'Identifier'
  ) {
    const objectPath = getStaticDependencyPath(unwrappedNode.object)
    return objectPath ? `${objectPath}.${unwrappedNode.property.name}` : null
  }

  return null
}

function isHookCallback(node) {
  return node.type === 'ArrowFunctionExpression' || node.type === 'FunctionExpression'
}

function isNode(value) {
  return Boolean(value && typeof value === 'object' && typeof value.type === 'string')
}

function collectReferencedPaths(node) {
  const paths = new Set()

  function visit(value) {
    if (!isNode(value)) {
      return
    }

    switch (value.type) {
      case 'Identifier':
        paths.add(value.name)
        return
      case 'ChainExpression':
      case 'TSAsExpression':
      case 'TSNonNullExpression':
      case 'TSSatisfiesExpression':
      case 'TSInstantiationExpression':
        visit(value.expression)
        return
      case 'MemberExpression': {
        const path = getStaticDependencyPath(value)
        if (path) {
          paths.add(path)
        }
        visit(value.object)
        if (value.computed) {
          visit(value.property)
        }
        return
      }
      case 'Property':
        if (value.computed) {
          visit(value.key)
        }
        visit(value.value)
        return
      case 'VariableDeclarator':
        visit(value.init)
        return
      case 'FunctionDeclaration':
      case 'FunctionExpression':
      case 'ArrowFunctionExpression':
        visit(value.body)
        return
      default:
        break
    }

    for (const [key, child] of Object.entries(value)) {
      if (
        key === 'type' ||
        key === 'loc' ||
        key === 'range' ||
        key === 'parent' ||
        key === 'start' ||
        key === 'end' ||
        key === 'id' ||
        key === 'key' ||
        key === 'params' ||
        key === 'typeAnnotation' ||
        key === 'typeParameters' ||
        key === 'returnType'
      ) {
        continue
      }

      if (Array.isArray(child)) {
        child.forEach(visit)
      } else {
        visit(child)
      }
    }
  }

  visit(node)
  return paths
}

function isDependencyPathUsed(dependencyPath, referencedPaths) {
  return Array.from(referencedPaths).some(
    (referencedPath) => referencedPath === dependencyPath || referencedPath.startsWith(`${dependencyPath}.`),
  )
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

            const dependencyArrayIndex = getHookDependencyArrayIndex(hookName)
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
    'no-unused-hook-dependencies': {
      meta: {
        type: 'problem',
        docs: {
          description: 'Disallow hook dependency array entries that are not referenced by the hook callback.',
        },
        schema: [],
        messages: {
          unusedDependency: "Hook dependency '{{dependency}}' is not referenced in the hook callback.",
        },
      },
      create(context) {
        return {
          CallExpression(node) {
            const hookName = getCalleeName(node.callee)
            if (!hookName || !dependencyHookNames.has(hookName)) {
              return
            }

            const callback = node.arguments[getHookCallbackIndex(hookName)]
            const dependencyArray = node.arguments[getHookDependencyArrayIndex(hookName)]
            if (!callback || !isHookCallback(callback) || !dependencyArray || dependencyArray.type !== 'ArrayExpression') {
              return
            }

            const referencedPaths = collectReferencedPaths(callback.body)
            dependencyArray.elements.forEach((dependency) => {
              if (!dependency || dependency.type === 'SpreadElement') {
                return
              }

              const dependencyPath = getStaticDependencyPath(dependency)
              if (!dependencyPath || isDependencyPathUsed(dependencyPath, referencedPaths)) {
                return
              }

              context.report({
                node: dependency,
                messageId: 'unusedDependency',
                data: {
                  dependency: dependencyPath,
                },
              })
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
      'local/no-unused-hook-dependencies': 'error',
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
