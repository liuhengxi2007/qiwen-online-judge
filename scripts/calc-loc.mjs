import { readFileSync, readdirSync, statSync } from 'node:fs'
import { extname, join, resolve } from 'node:path'

const root = process.cwd()

const textExtensions = new Set(['.scala', '.ts', '.tsx', '.css'])

const profiles = {
  business: {
    label: 'Business Code',
    include: [
      'backend/src/main/scala',
      'contracts',
      'frontend/src/features',
      'frontend/src/shared',
      'frontend/src/router.tsx',
    ],
  },
  app: {
    label: 'App Source',
    include: [
      'backend/src/main/scala',
      'contracts',
      'frontend/src',
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
  business  backend/src/main/scala + contracts + frontend feature/shared code + router
  app       backend/src/main/scala + contracts + all frontend/src code
  all       print both business and app profiles
`)
}

function collectFiles(relativePath) {
  const absolutePath = resolve(root, relativePath)
  const stats = statSync(absolutePath)

  if (stats.isFile()) {
    return isCountableFile(absolutePath) ? [relativePath] : []
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
  return textExtensions.has(extname(pathname))
}

function countLines(relativePath) {
  const source = readFileSync(resolve(root, relativePath), 'utf8')

  if (source.length === 0) {
    return 0
  }

  return source.split('\n').length
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
    contracts: 0,
    frontend: 0,
  }

  for (const entry of counts) {
    if (entry.file.startsWith('backend/')) {
      groups.backend += entry.lines
    } else if (entry.file.startsWith('contracts/')) {
      groups.contracts += entry.lines
    } else if (entry.file.startsWith('frontend/')) {
      groups.frontend += entry.lines
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
  console.log(`  contracts: ${formatNumber(summary.groups.contracts)} LoC`)
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
