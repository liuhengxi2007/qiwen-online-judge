import { readFileSync, readdirSync, statSync } from 'node:fs'
import { extname, join, resolve } from 'node:path'

const root = process.cwd()

const textExtensions = new Set(['.scala', '.ts', '.tsx', '.css'])

const profiles = {
  business: {
    label: 'Business Code',
    include: [
      'backend/src/main/scala',
      'frontend/src/apis',
      'frontend/src/objects',
      'frontend/src/pages',
      'frontend/src/system',
      'judger/src/main/scala',
    ],
  },
  app: {
    label: 'App Source',
    include: [
      'backend/src/main/scala',
      'frontend/src',
      'judger/src/main/scala',
    ],
  },
}

function parseArgs(argv) {
  let mode = 'all'
  let json = false

  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index]

    if (argument === '--json') {
      json = true
      continue
    }

    if (argument === '--mode') {
      mode = argv[index + 1] ?? ''
      index += 1
      continue
    }

    if (argument.startsWith('--mode=')) {
      mode = argument.slice('--mode='.length)
      continue
    }

    if (argument === '--help' || argument === '-h') {
      printHelp()
      process.exit(0)
    }
  }

  if (mode !== 'all' && !(mode in profiles)) {
    console.error(`Unknown mode: ${mode}`)
    printHelp()
    process.exit(1)
  }

  return { mode, json }
}

function printHelp() {
  console.log(`Usage:
  node scripts/calc-loc.mjs
  node scripts/calc-loc.mjs --mode business
  node scripts/calc-loc.mjs --mode app
  node scripts/calc-loc.mjs --json

Modes:
  business  backend/src/main/scala + frontend app layers + judger/src/main/scala
  app       backend/src/main/scala + all frontend/src code + judger/src/main/scala
  all       print both business and app profiles
`)
}

function collectFiles(relativePath) {
  const absolutePath = resolve(root, relativePath)
  const stats = statSync(absolutePath)

  if (stats.isFile()) {
    return isCountableFile(relativePath) ? [relativePath] : []
  }

  return walkDirectory(relativePath)
}

function walkDirectory(relativeDirectory) {
  const absoluteDirectory = resolve(root, relativeDirectory)
  const entries = readdirSync(absoluteDirectory, { withFileTypes: true })
  const files = []

  for (const entry of entries) {
    const relativePath = join(relativeDirectory, entry.name)

    if (entry.isDirectory()) {
      files.push(...walkDirectory(relativePath))
      continue
    }

    if (entry.isFile() && isCountableFile(relativePath)) {
      files.push(relativePath)
    }
  }

  return files
}

function isCountableFile(pathname) {
  return textExtensions.has(extname(pathname)) && !isTestFile(pathname)
}

function isTestFile(pathname) {
  const normalizedPath = pathname.split('\\').join('/')
  const filename = normalizedPath.slice(normalizedPath.lastIndexOf('/') + 1)

  return (
    /(^|\/)(test|tests|__tests__)\//.test(normalizedPath) ||
    /\.(test|spec)\.[^.]+$/.test(filename)
  )
}

function countLines(relativePath) {
  const source = readFileSync(resolve(root, relativePath), 'utf8')

  return countCodeLines(source, {
    supportsLineComment: extname(relativePath) !== '.css',
  })
}

function countCodeLines(source, { supportsLineComment }) {
  let total = 0
  let lineHasCode = false
  let state = 'code'
  let quote = ''
  let escaped = false

  const finishLine = () => {
    if (lineHasCode) {
      total += 1
    }

    lineHasCode = false
  }

  for (let index = 0; index < source.length; index += 1) {
    const character = source[index]
    const next = source[index + 1]

    if (character === '\r') {
      continue
    }

    if (state === 'lineComment') {
      if (character === '\n') {
        finishLine()
        state = 'code'
      }

      continue
    }

    if (state === 'blockComment') {
      if (character === '\n') {
        finishLine()
        continue
      }

      if (character === '*' && next === '/') {
        index += 1
        state = 'code'
      }

      continue
    }

    if (state === 'string') {
      if (character === '\n') {
        finishLine()
        escaped = false

        if (quote !== '`') {
          state = 'code'
          quote = ''
        }

        continue
      }

      if (!/\s/.test(character)) {
        lineHasCode = true
      }

      if (escaped) {
        escaped = false
        continue
      }

      if (character === '\\') {
        escaped = true
        continue
      }

      if (character === quote) {
        state = 'code'
        quote = ''
      }

      continue
    }

    if (state === 'tripleString') {
      if (character === '\n') {
        finishLine()
        continue
      }

      if (!/\s/.test(character)) {
        lineHasCode = true
      }

      if (character === '"' && next === '"' && source[index + 2] === '"') {
        index += 2
        state = 'code'
      }

      continue
    }

    if (character === '\n') {
      finishLine()
      continue
    }

    if (supportsLineComment && character === '/' && next === '/') {
      index += 1
      state = 'lineComment'
      continue
    }

    if (character === '/' && next === '*') {
      index += 1
      state = 'blockComment'
      continue
    }

    if (character === '"' && next === '"' && source[index + 2] === '"') {
      lineHasCode = true
      index += 2
      state = 'tripleString'
      continue
    }

    if (character === '"' || character === "'" || character === '`') {
      lineHasCode = true
      state = 'string'
      quote = character
      escaped = false
      continue
    }

    if (!/\s/.test(character)) {
      lineHasCode = true
    }
  }

  finishLine()
  return total
}

function summarizeProfile(profile) {
  const files = profile.include
    .flatMap((entry) => collectFiles(entry))
    .sort()

  const counts = files.map((file) => ({
    file,
    lines: countLines(file),
  }))

  const totalLines = counts.reduce((sum, entry) => sum + entry.lines, 0)

  const groups = {
    backend: 0,
    frontend: 0,
    judger: 0,
  }

  for (const entry of counts) {
    if (entry.file.startsWith('backend/')) {
      groups.backend += entry.lines
    } else if (entry.file.startsWith('frontend/')) {
      groups.frontend += entry.lines
    } else if (entry.file.startsWith('judger/')) {
      groups.judger += entry.lines
    }
  }

  return {
    label: profile.label,
    totalLines,
    fileCount: counts.length,
    groups,
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat('en-US').format(value)
}

function printSummary(name, summary) {
  console.log(`${summary.label} (${name})`)
  console.log(`  total: ${formatNumber(summary.totalLines)} LoC`)
  console.log(`  backend: ${formatNumber(summary.groups.backend)} LoC`)
  console.log(`  frontend: ${formatNumber(summary.groups.frontend)} LoC`)
  console.log(`  judger: ${formatNumber(summary.groups.judger)} LoC`)
  console.log(`  files: ${formatNumber(summary.fileCount)}`)
}

function main() {
  const { mode, json } = parseArgs(process.argv.slice(2))
  const selectedProfiles = mode === 'all' ? Object.entries(profiles) : [[mode, profiles[mode]]]

  const result = Object.fromEntries(
    selectedProfiles.map(([name, profile]) => [name, summarizeProfile(profile)]),
  )

  if (json) {
    console.log(JSON.stringify(result, null, 2))
    return
  }

  for (const [index, [name, summary]] of Object.entries(result).entries()) {
    if (index > 0) {
      console.log('')
    }

    printSummary(name, summary)
  }
}

main()
