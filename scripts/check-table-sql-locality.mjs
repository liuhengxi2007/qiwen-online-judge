#!/usr/bin/env node

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { extname, join, resolve, sep } from 'node:path'

const root = process.cwd()
const checkedRoots = [
  'backend/src/main/scala/domains',
  'backend/src/main/scala/database/table',
]

function normalizePath(path) {
  return path.split(sep).join('/')
}

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
}

function walk(relativePath, extension) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    return []
  }

  return readdirSync(absolutePath)
    .flatMap((entry) => {
      const child = join(relativePath, entry)
      const childAbsolute = resolve(root, child)
      if (statSync(childAbsolute).isDirectory()) {
        return walk(child, extension)
      }
      return extname(child) === extension ? [normalizePath(child)] : []
    })
    .sort()
}

function lineNumber(source, index) {
  return source.slice(0, index).split('\n').length
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function isSchemaFile(filePath) {
  return /TableSchema\.scala$/.test(filePath)
}

function isTableAreaFile(filePath) {
  return filePath.includes('/table/') || filePath.includes('/database/table/')
}

function isTopLevelMemberLine(line) {
  return /^  (?:private\s+)?(?:val|def|enum)\s+/.test(line)
}

function memberKind(line) {
  const match = line.match(/^  (?:private\s+)?(val|def|enum)\s+/)
  return match?.[1] ?? null
}

function memberName(line) {
  const match = line.match(/^  (?:private\s+)?(?:val|def|enum)\s+([A-Za-z_]\w*)/)
  return match?.[1] ?? null
}

function isSqlMember(member) {
  return (
    member.kind !== 'enum' &&
    (
      /(?:SQL|Sql|sql)$/.test(member.name) ||
      /^sql\d*$/i.test(member.name) ||
      (member.kind === 'val' && /:\s*String\b/.test(member.text) && /s?"""/.test(member.text))
    )
  )
}

function extractMembers(source) {
  const lines = source.split('\n')
  const members = []
  let offset = 0

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    if (!isTopLevelMemberLine(line)) {
      offset += line.length + 1
      continue
    }

    const startIndex = offset
    const startLine = index + 1
    const kind = memberKind(line)
    const name = memberName(line)

    let end = index + 1
    let endOffset = offset + line.length + 1
    while (end < lines.length && !isTopLevelMemberLine(lines[end])) {
      endOffset += lines[end].length + 1
      end += 1
    }

    if (kind && name) {
      const text = source.slice(startIndex, endOffset)
      members.push({
        kind,
        name,
        startLine,
        startIndex,
        text,
        isSql: false,
      })
    }

    while (index + 1 < end) {
      index += 1
    }
    offset = endOffset
  }

  for (const member of members) {
    member.isSql = isSqlMember(member)
  }

  return members
}

function containsIdentifier(text, name) {
  return new RegExp(`\\b${escapeRegExp(name)}\\b`).test(text)
}

function sqlGroupBefore(members, defIndex) {
  const group = []
  for (let index = defIndex - 1; index >= 0; index -= 1) {
    if (!members[index].isSql) {
      break
    }
    group.unshift(members[index])
  }
  return group
}

function checkFile(filePath, errors) {
  if (!isTableAreaFile(filePath)) {
    return
  }

  if (/TableSql\.scala$/.test(filePath)) {
    errors.push(`${filePath}: operation SQL must live next to its table methods, not in *TableSql.scala`)
    return
  }

  if (isSchemaFile(filePath)) {
    return
  }

  const source = read(filePath)
  const members = extractMembers(source)
  const sqlMembers = members.filter((member) => member.isSql)
  const sqlNames = new Set(sqlMembers.map((member) => member.name))
  const groupedSqlNames = new Set()

  for (let index = 0; index < members.length; index += 1) {
    const member = members[index]
    if (member.kind !== 'def' || member.isSql) {
      continue
    }

    const group = sqlGroupBefore(members, index)
    const groupNames = new Set(group.map((entry) => entry.name))
    const groupAndMethodText = `${group.map((entry) => entry.text).join('\n')}\n${member.text}`

    for (const entry of group) {
      groupedSqlNames.add(entry.name)
      const laterGroupAndMethodText = groupAndMethodText.slice(
        Math.max(0, groupAndMethodText.indexOf(entry.text) + entry.text.length),
      )
      if (!containsIdentifier(laterGroupAndMethodText, entry.name)) {
        errors.push(
          `${filePath}:${entry.startLine} SQL declaration "${entry.name}" is in front of "${member.name}", but that group does not reference it`,
        )
      }
    }

    for (const sqlName of sqlNames) {
      if (containsIdentifier(member.text, sqlName) && !groupNames.has(sqlName)) {
        errors.push(
          `${filePath}:${member.startLine} method "${member.name}" references "${sqlName}", but that SQL is not in the contiguous block immediately before the method`,
        )
      }
    }
  }

  for (const member of sqlMembers) {
    if (!groupedSqlNames.has(member.name)) {
      errors.push(`${filePath}:${member.startLine} SQL declaration "${member.name}" is not immediately before a method`)
    }
  }
}

function run() {
  const errors = []
  const files = checkedRoots.flatMap((rootPath) => walk(rootPath, '.scala'))

  for (const filePath of files) {
    checkFile(filePath, errors)
  }

  if (errors.length > 0) {
    console.error('Table SQL locality check failed:')
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exit(1)
  }

  console.log('Table SQL locality check passed.')
}

run()
