import { parseDocument } from 'yaml'

import { parseProblemDataPath, problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/ProblemDataTreeNode'

export const judgeConfigPath = 'judge.yaml' as const

export const judgeConfigTemplate = `version: 1
roundingScale: 6

limits:
  timeMs: 1000
  memoryMb: 256

checker:
  type: builtin
  name: exact

aggregation:
  testcases: sum_max_max
  subtasks: sum_max_max

subtasks:
  - name: sample
    scoreRatio: 0.2
    testcases:
      - name: sample-1
        input: sample/1.in
        answer: sample/1.ans

  - name: main
    scoreRatio: 0.8
    testcases:
      - name: "1"
        input: tests/1.in
        answer: tests/1.ans
`

const aggregations = new Set(['min_max_max', 'min_sum_max', 'sum_max_max', 'sum_sum_max'])

export type JudgeConfigValidationResult =
  | { ok: true; warnings: string[] }
  | { ok: false; errors: string[]; warnings: string[] }

type ValidationContext = {
  errors: string[]
  filePaths: Set<string>
}

type LimitsConfig = {
  timeMs: number
  memoryMb: number
}

type CheckerConfig =
  | { type: 'builtin'; name: 'exact' }
  | { type: 'cpp'; path: string }

type AggregationConfig = {
  testcases?: string
  subtasks?: string
}

export function validateJudgeConfigYaml(
  content: string,
  dataTree: ProblemDataTreeNode[],
): JudgeConfigValidationResult {
  const document = parseDocument(content, { prettyErrors: false })
  const syntaxErrors = document.errors.map((error) => error.message)
  if (syntaxErrors.length > 0) {
    return { ok: false, errors: syntaxErrors, warnings: [] }
  }

  const root = document.toJSON()
  const ctx: ValidationContext = {
    errors: [],
    filePaths: new Set(
      dataTree
        .filter((node) => node.kind === 'file')
        .map((node) => problemDataPathValue(node.path)),
    ),
  }

  if (!isRecord(root)) {
    ctx.errors.push('judge.yaml must be an object.')
    return toResult(ctx)
  }

  requireExactNumber(root.version, 'version', 1, ctx)
  validateOptionalInteger(root.roundingScale, 'roundingScale', 0, 18, ctx)

  const rootLimits = validateLimits(root.limits, 'limits', ctx)
  const rootChecker = validateChecker(root.checker, 'checker', ctx)
  const rootAggregation = validateAggregation(root.aggregation, 'aggregation', ctx)
  const subtasks = validateList(root.subtasks, 'subtasks', ctx)
  if (subtasks && subtasks.length === 0) {
    ctx.errors.push('subtasks must contain at least one item.')
  }

  if (subtasks) {
    validateSiblingRatios(subtasks, 'subtasks', ctx)
    subtasks.forEach((subtask, index) => {
      if (!isRecord(subtask)) {
        ctx.errors.push(`subtasks[${index}] must be an object.`)
        return
      }

      const label = stringOrFallback(subtask.name, `subtasks[${index}]`)
      const limits = validateLimits(subtask.limits, `${label}.limits`, ctx) ?? rootLimits
      const checker = validateChecker(subtask.checker, `${label}.checker`, ctx) ?? rootChecker
      const aggregation = mergeAggregation(rootAggregation, validateAggregation(subtask.aggregation, `${label}.aggregation`, ctx))
      const testcases = validateList(subtask.testcases, `${label}.testcases`, ctx)

      if (testcases && testcases.length === 0) {
        ctx.errors.push(`${label}.testcases must contain at least one item.`)
      }
      if (testcases) {
        validateSiblingRatios(testcases, `${label}.testcases`, ctx)
        testcases.forEach((testcase, testcaseIndex) => {
          if (!isRecord(testcase)) {
            ctx.errors.push(`${label}.testcases[${testcaseIndex}] must be an object.`)
            return
          }

          const testcaseLabel = `${label}/${stringOrFallback(testcase.name, `testcase-${testcaseIndex + 1}`)}`
          const testcaseLimits = validateLimits(testcase.limits, `${testcaseLabel}.limits`, ctx) ?? limits
          const testcaseChecker = validateChecker(testcase.checker, `${testcaseLabel}.checker`, ctx) ?? checker
          validateAggregation(testcase.aggregation, `${testcaseLabel}.aggregation`, ctx)

          if (!testcaseLimits) {
            ctx.errors.push(`Limits are required for testcase ${testcaseLabel}.`)
          }
          if (!testcaseChecker) {
            ctx.errors.push(`Checker is required for testcase ${testcaseLabel}.`)
          }
          if (!aggregation.testcases) {
            ctx.errors.push(`Testcase aggregation is required for subtask ${label}.`)
          }

          validateOptionalFileRef(testcase.input, `${testcaseLabel}.input`, ctx)
          validateRequiredFileRef(testcase.answer, `${testcaseLabel}.answer`, ctx)
          validateCheckerFileRef(testcaseChecker, `${testcaseLabel}.checker`, ctx)
        })
      }
    })
  }

  return toResult(ctx)
}

function validateLimits(value: unknown, label: string, ctx: ValidationContext): LimitsConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return null
  }

  const timeMs = validatePositiveInteger(value.timeMs, `${label}.timeMs`, ctx)
  const memoryMb = validatePositiveInteger(value.memoryMb, `${label}.memoryMb`, ctx)
  return timeMs !== null && memoryMb !== null ? { timeMs, memoryMb } : null
}

function validateChecker(value: unknown, label: string, ctx: ValidationContext): CheckerConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return null
  }

  if (value.type === 'builtin') {
    if (value.name === 'exact') {
      return { type: 'builtin', name: 'exact' }
    }
    ctx.errors.push(`${label}.name must be exact for builtin checker.`)
    return null
  }

  if (value.type === 'cpp') {
    if (typeof value.path !== 'string' || value.path.trim() === '') {
      ctx.errors.push(`${label}.path is required for C++ checker.`)
      return null
    }
    const parsedPath = parseProblemDataPath(value.path)
    if (!parsedPath.ok) {
      ctx.errors.push(`${label}.path is invalid: ${parsedPath.error}`)
      return null
    }
    return { type: 'cpp', path: problemDataPathValue(parsedPath.value) }
  }

  ctx.errors.push(`${label}.type must be builtin or cpp.`)
  return null
}

function validateAggregation(value: unknown, label: string, ctx: ValidationContext): AggregationConfig {
  if (value === undefined) {
    return {}
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return {}
  }

  return {
    testcases: validateAggregationValue(value.testcases, `${label}.testcases`, ctx),
    subtasks: validateAggregationValue(value.subtasks, `${label}.subtasks`, ctx),
  }
}

function validateAggregationValue(value: unknown, label: string, ctx: ValidationContext): string | undefined {
  if (value === undefined) {
    return undefined
  }
  if (typeof value !== 'string' || !aggregations.has(value)) {
    ctx.errors.push(`${label} must be one of: ${Array.from(aggregations).join(', ')}.`)
    return undefined
  }
  return value
}

function mergeAggregation(parent: AggregationConfig, child: AggregationConfig): AggregationConfig {
  return {
    testcases: child.testcases ?? parent.testcases,
    subtasks: child.subtasks ?? parent.subtasks,
  }
}

function validateSiblingRatios(items: unknown[], label: string, ctx: ValidationContext): void {
  let explicitSum = 0

  items.forEach((item, index) => {
    if (!isRecord(item) || item.scoreRatio === undefined) {
      return
    }
    if (typeof item.scoreRatio !== 'number' || !Number.isFinite(item.scoreRatio)) {
      ctx.errors.push(`${label}[${index}].scoreRatio must be a number.`)
      return
    }
    if (item.scoreRatio < 0 || item.scoreRatio > 1) {
      ctx.errors.push(`${label}[${index}].scoreRatio must be between 0 and 1.`)
      return
    }
    explicitSum += item.scoreRatio
  })

  if (explicitSum > 1) {
    ctx.errors.push(`${label} explicit scoreRatio values must not sum above 1.`)
  }
}

function validateRequiredFileRef(value: unknown, label: string, ctx: ValidationContext): void {
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} is required.`)
    return
  }
  validateExistingFile(value, label, ctx)
}

function validateOptionalFileRef(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} must be a nonempty string when present.`)
    return
  }
  validateExistingFile(value, label, ctx)
}

function validateCheckerFileRef(checker: CheckerConfig | null | undefined, label: string, ctx: ValidationContext): void {
  if (checker?.type !== 'cpp') {
    return
  }
  validateExistingFile(checker.path, `${label}.path`, ctx)
}

function validateExistingFile(rawPath: string, label: string, ctx: ValidationContext): void {
  const parsedPath = parseProblemDataPath(rawPath)
  if (!parsedPath.ok) {
    ctx.errors.push(`${label} is invalid: ${parsedPath.error}`)
    return
  }

  const path = problemDataPathValue(parsedPath.value)
  if (!ctx.filePaths.has(path)) {
    ctx.errors.push(`${label} does not exist: ${path}.`)
  }
}

function requireExactNumber(value: unknown, label: string, expected: number, ctx: ValidationContext): void {
  if (value !== expected) {
    ctx.errors.push(`${label} must be ${expected}.`)
  }
}

function validateOptionalInteger(
  value: unknown,
  label: string,
  min: number,
  max: number,
  ctx: ValidationContext,
): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    ctx.errors.push(`${label} must be an integer.`)
    return
  }
  if (value < min || value > max) {
    ctx.errors.push(`${label} must be between ${min} and ${max}.`)
  }
}

function validatePositiveInteger(value: unknown, label: string, ctx: ValidationContext): number | null {
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    ctx.errors.push(`${label} must be an integer.`)
    return null
  }
  if (value <= 0) {
    ctx.errors.push(`${label} must be greater than 0.`)
    return null
  }
  return value
}

function validateList(value: unknown, label: string, ctx: ValidationContext): unknown[] | null {
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} is required and must be a list.`)
    return null
  }
  return value
}

function stringOrFallback(value: unknown, fallback: string): string {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function toResult(ctx: ValidationContext): JudgeConfigValidationResult {
  return ctx.errors.length > 0 ? { ok: false, errors: ctx.errors, warnings: [] } : { ok: true, warnings: [] }
}
