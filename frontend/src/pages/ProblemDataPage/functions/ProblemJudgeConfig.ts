import { parseDocument } from 'yaml'

import { parseProblemDataPath, problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

export const judgeConfigPath = 'judge.yaml' as const

export const judgeConfigTemplate = `version: 2
roundingScale: 6

mode:
  type: traditional
  role: main

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
  - label: sample
    scoreRatio: 0.2
    testcases:
      - label: sample-1
        input: sample/1.in
        answer: sample/1.ans

  - label: main
    scoreRatio: 0.8
    testcases:
      - label: "1"
        input: tests/1.in
        answer: tests/1.ans
`

const aggregations = new Set(['min_max_max', 'min_sum_max', 'sum_max_max', 'sum_sum_max'])
const rolePattern = /^[A-Za-z0-9_-]+$/

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

type ToolLimitsConfig = {
  realTimeMs: number
  memoryMb: number
}

type ToolConfig = {
  path: string
  limits?: ToolLimitsConfig
}

type CheckerConfig =
  | { type: 'builtin'; name: 'exact' }
  | { type: 'builtin'; name: 'echo' }
  | { type: 'cpp17'; path: string }

type ModeConfig =
  | { type: 'traditional'; role: string }
  | { type: 'interactive'; roles: string[]; interactor: ToolConfig }

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

  requireExactNumber(root.version, 'version', 2, ctx)
  validateOptionalInteger(root.roundingScale, 'roundingScale', 0, 18, ctx)

  const rootLimits = validateLimits(root.limits, 'limits', ctx)
  const rootChecker = validateChecker(root.checker, 'checker', ctx)
  validateTool(root.validator, 'validator', ctx)
  const rootMode = validateMode(root.mode, 'mode', ctx) ?? { type: 'traditional' as const, role: 'main' }
  validateLimitedTool(root.strategyProvider, 'strategyProvider', ctx)
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

      const subtaskLabel = judgeNodeLabel('subtask', index + 1, subtask.label)
      rejectLegacyName(subtask, subtaskLabel, ctx)
      validateOptionalLabel(subtask.label, `subtasks[${index}].label`, ctx)
      const limits = validateLimits(subtask.limits, `${subtaskLabel}.limits`, ctx) ?? rootLimits
      const checker = validateChecker(subtask.checker, `${subtaskLabel}.checker`, ctx) ?? rootChecker
      validateTool(subtask.validator, `${subtaskLabel}.validator`, ctx)
      const mode = validateMode(subtask.mode, `${subtaskLabel}.mode`, ctx) ?? rootMode
      validateLimitedTool(subtask.strategyProvider, `${subtaskLabel}.strategyProvider`, ctx)
      const aggregation = mergeAggregation(rootAggregation, validateAggregation(subtask.aggregation, `${subtaskLabel}.aggregation`, ctx))
      const testcases = validateList(subtask.testcases, `${subtaskLabel}.testcases`, ctx)

      if (testcases && testcases.length === 0) {
        ctx.errors.push(`${subtaskLabel}.testcases must contain at least one item.`)
      }
      if (testcases) {
        validateSiblingRatios(testcases, `${subtaskLabel}.testcases`, ctx)
        testcases.forEach((testcase, testcaseIndex) => {
          if (!isRecord(testcase)) {
            ctx.errors.push(`${subtaskLabel}.testcases[${testcaseIndex}] must be an object.`)
            return
          }

          const testcaseLabel = `${subtaskLabel} ${judgeNodeLabel('testcase', testcaseIndex + 1, testcase.label)}`
          rejectLegacyName(testcase, testcaseLabel, ctx)
          validateOptionalLabel(testcase.label, `${testcaseLabel}.label`, ctx)
          if (testcase.mode !== undefined) {
            ctx.errors.push(`${testcaseLabel}.mode cannot be declared on a testcase.`)
          }
          if (testcase.validator !== undefined) {
            ctx.errors.push(`${testcaseLabel}.validator cannot be declared on a testcase.`)
          }
          const testcaseLimits = validateLimits(testcase.limits, `${testcaseLabel}.limits`, ctx) ?? limits
          const testcaseChecker = validateChecker(testcase.checker, `${testcaseLabel}.checker`, ctx) ?? checker
          if (testcase.strategyProvider !== undefined) {
            validateLimitedTool(testcase.strategyProvider, `${testcaseLabel}.strategyProvider`, ctx)
          }
          validateAggregation(testcase.aggregation, `${testcaseLabel}.aggregation`, ctx)

          if (!testcaseLimits) {
            ctx.errors.push(`Limits are required for ${testcaseLabel}.`)
          }
          if (!testcaseChecker) {
            ctx.errors.push(`Checker is required for ${testcaseLabel}.`)
          }
          if (!aggregation.testcases) {
            ctx.errors.push(`Testcase aggregation is required for ${subtaskLabel}.`)
          }
          if (mode.type === 'interactive' && mode.roles.length === 0) {
            ctx.errors.push(`${subtaskLabel}.mode.roles must contain at least one role.`)
          }

          validateRequiredFileRef(testcase.input, `${testcaseLabel}.input`, ctx)
          validateOptionalFileRef(testcase.answer, `${testcaseLabel}.answer`, ctx)
          if (testcaseChecker?.type === 'builtin' && testcaseChecker.name === 'exact' && testcase.answer === undefined) {
            ctx.errors.push(`${testcaseLabel}.answer is required for builtin exact checker.`)
          }
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

  const timeMs = validateIntegerRange(value.timeMs, `${label}.timeMs`, 1, 600000, ctx)
  const memoryMb = validateIntegerRange(value.memoryMb, `${label}.memoryMb`, 1, 65536, ctx)
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
    if (value.name === 'exact' || value.name === 'echo') {
      return { type: 'builtin', name: value.name }
    }
    ctx.errors.push(`${label}.name must be exact or echo for builtin checker.`)
    return null
  }

  if (value.type === 'cpp17' || value.type === 'cpp') {
    const path = validatePathValue(value.path, `${label}.path`, ctx)
    return path ? { type: 'cpp17', path } : null
  }

  ctx.errors.push(`${label}.type must be builtin or cpp17.`)
  return null
}

function validateTool(value: unknown, label: string, ctx: ValidationContext): ToolConfig | null {
  if (value === undefined) {
    return null
  }
  if (typeof value === 'string') {
    const path = validatePathValue(value, label, ctx)
    return path ? { path } : null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be a path string or an object with a path.`)
    return null
  }
  const path = validatePathValue(value.path, `${label}.path`, ctx)
  return path ? { path } : null
}

function validateLimitedTool(value: unknown, label: string, ctx: ValidationContext): ToolConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object with path and limits.`)
    return null
  }

  const path = validatePathValue(value.path, `${label}.path`, ctx)
  const limits = validateToolLimits(value.limits, `${label}.limits`, ctx)
  return path && limits ? { path, limits } : null
}

function validateToolLimits(value: unknown, label: string, ctx: ValidationContext): ToolLimitsConfig | null {
  if (!isRecord(value)) {
    ctx.errors.push(`${label} is required and must be an object.`)
    return null
  }

  const realTimeMs = validateIntegerRange(value.realTimeMs, `${label}.realTimeMs`, 1, 600000, ctx)
  const memoryMb = validateIntegerRange(value.memoryMb, `${label}.memoryMb`, 1, 65536, ctx)
  return realTimeMs !== null && memoryMb !== null ? { realTimeMs, memoryMb } : null
}

function validateMode(value: unknown, label: string, ctx: ValidationContext): ModeConfig | null {
  if (value === undefined) {
    return null
  }
  if (typeof value === 'string') {
    if (value === 'traditional') {
      return { type: 'traditional', role: 'main' }
    }
    ctx.errors.push(`${label} must be an object for interactive mode.`)
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be a string or an object.`)
    return null
  }

  if (value.type === 'traditional') {
    const role = validateRole(typeof value.role === 'string' ? value.role : 'main', `${label}.role`, ctx)
    return role ? { type: 'traditional', role } : null
  }

  if (value.type === 'interactive') {
    const roles = validateRoleList(value.roles, `${label}.roles`, ctx)
    const interactor = validateLimitedTool(value.interactor, `${label}.interactor`, ctx)
    if (value.interactor === undefined) {
      ctx.errors.push(`${label}.interactor is required.`)
    }
    return roles && interactor ? { type: 'interactive', roles, interactor } : null
  }

  ctx.errors.push(`${label}.type must be traditional or interactive.`)
  return null
}

function validateRoleList(value: unknown, label: string, ctx: ValidationContext): string[] | null {
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} is required and must be a list.`)
    return null
  }
  const roles = value.flatMap((item, index) => {
    if (typeof item !== 'string') {
      ctx.errors.push(`${label}[${index}] must be a string.`)
      return []
    }
    const role = validateRole(item, `${label}[${index}]`, ctx)
    return role ? [role] : []
  })
  if (roles.length === 0) {
    ctx.errors.push(`${label} must contain at least one role.`)
  }
  return roles
}

function validateRole(value: string, label: string, ctx: ValidationContext): string | null {
  const role = value.trim()
  if (!role || !rolePattern.test(role)) {
    ctx.errors.push(`${label} must contain only ASCII letters, digits, "_" or "-".`)
    return null
  }
  return role
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

function validatePathValue(value: unknown, label: string, ctx: ValidationContext): string | null {
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} is required.`)
    return null
  }
  return validateExistingFile(value, label, ctx)
}

function validateExistingFile(rawPath: string, label: string, ctx: ValidationContext): string | null {
  const parsedPath = parseProblemDataPath(rawPath)
  if (!parsedPath.ok) {
    ctx.errors.push(`${label} is invalid: ${parsedPath.error}`)
    return null
  }

  const path = problemDataPathValue(parsedPath.value)
  if (!ctx.filePaths.has(path)) {
    ctx.errors.push(`${label} does not exist: ${path}.`)
    return null
  }
  return path
}

function rejectLegacyName(value: Record<string, unknown>, label: string, ctx: ValidationContext): void {
  if (value.name !== undefined) {
    ctx.errors.push(`${label}.name is not supported in judge.yaml v2; use label instead.`)
  }
}

function validateOptionalLabel(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} must be a nonempty string when present.`)
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

function validateIntegerRange(
  value: unknown,
  label: string,
  min: number,
  max: number,
  ctx: ValidationContext,
): number | null {
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    ctx.errors.push(`${label} must be an integer.`)
    return null
  }
  if (value < min || value > max) {
    ctx.errors.push(`${label} must be between ${min} and ${max}.`)
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

function judgeNodeLabel(kind: 'subtask' | 'testcase', index: number, label: unknown): string {
  return typeof label === 'string' && label.trim() ? `${kind} ${index} (${label.trim()})` : `${kind} ${index}`
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function toResult(ctx: ValidationContext): JudgeConfigValidationResult {
  return ctx.errors.length > 0 ? { ok: false, errors: ctx.errors, warnings: [] } : { ok: true, warnings: [] }
}
