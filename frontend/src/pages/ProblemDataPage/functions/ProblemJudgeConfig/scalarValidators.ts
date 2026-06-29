import type { IntegerRangeValidationInput } from './objects/JudgeConfigLimits'
import type { ValidationContext } from './objects/JudgeConfigValidation'

/**
 * 将未知 YAML 节点收窄为普通对象，排除 null 和数组。
 */
export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/**
 * 拒绝 v1 遗留 name 字段，引导用户在 judge.yaml v2 中使用 label。
 */
export function rejectLegacyName(value: Record<string, unknown>, label: string, ctx: ValidationContext): void {
  if (value.name !== undefined) {
    ctx.errors.push(`${label}.name is not supported in judge.yaml v2; use label instead.`)
  }
}

/**
 * 校验可选 label 字段，存在时必须是非空字符串。
 */
export function validateOptionalLabel(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} must be a nonempty string when present.`)
  }
}

/**
 * 校验数字字段是否等于指定版本或常量值。
 */
export function requireExactNumber(value: unknown, label: string, expected: number, ctx: ValidationContext): void {
  if (value !== expected) {
    ctx.errors.push(`${label} must be ${expected}.`)
  }
}

/**
 * 校验可选整数范围；缺失时跳过，存在时必须为指定闭区间内整数。
 */
export function validateOptionalInteger(input: IntegerRangeValidationInput): void {
  if (input.value === undefined) {
    return
  }
  if (typeof input.value !== 'number' || !Number.isInteger(input.value)) {
    input.ctx.errors.push(`${input.label} must be an integer.`)
    return
  }
  if (input.value < input.range.min || input.value > input.range.max) {
    input.ctx.errors.push(`${input.label} must be between ${input.range.min} and ${input.range.max}.`)
  }
}

/**
 * 校验可选布尔字段；缺失返回 null，非法类型记录错误。
 */
export function validateOptionalBoolean(value: unknown, label: string, ctx: ValidationContext): boolean | null {
  if (value === undefined) {
    return null
  }
  if (typeof value !== 'boolean') {
    ctx.errors.push(`${label} must be a boolean.`)
    return null
  }
  return value
}

/**
 * 校验必填整数范围，非法时记录错误并返回 null。
 */
export function validateIntegerRange(input: IntegerRangeValidationInput): number | null {
  if (typeof input.value !== 'number' || !Number.isInteger(input.value)) {
    input.ctx.errors.push(`${input.label} must be an integer.`)
    return null
  }
  if (input.value < input.range.min || input.value > input.range.max) {
    input.ctx.errors.push(`${input.label} must be between ${input.range.min} and ${input.range.max}.`)
    return null
  }
  return input.value
}

/**
 * 校验必填列表字段，非法时记录错误并返回 null。
 */
export function validateList(value: unknown, label: string, ctx: ValidationContext): unknown[] | null {
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} is required and must be a list.`)
    return null
  }
  return value
}
