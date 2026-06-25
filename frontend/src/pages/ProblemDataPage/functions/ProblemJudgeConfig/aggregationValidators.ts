import { aggregations, testcaseTypes } from './constants'
import type { AggregationConfig, DecimalUnits, IndexedValue, ValidationContext } from './types'
import { isRecord } from './utils'

/**
 * 校验 aggregation 对象，分别读取 testcase 和 subtask 聚合策略。
 */
export function validateAggregation(value: unknown, label: string, ctx: ValidationContext): AggregationConfig {
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

/**
 * 校验单个聚合策略值，缺失时返回 undefined 以便后续继承父级。
 */
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

/**
 * 校验测试点类型；缺失时按 main 处理，非法值记录错误并返回 null。
 */
export function validateTestcaseType(value: unknown, label: string, ctx: ValidationContext): 'main' | 'sample' | 'hack' | null {
  if (value === undefined) {
    return 'main'
  }
  if (typeof value !== 'string' || !testcaseTypes.has(value)) {
    ctx.errors.push(`${label} must be one of: main, sample, hack.`)
    return null
  }
  return value as 'main' | 'sample' | 'hack'
}

/**
 * 合并父级和子级聚合策略，子级声明优先。
 */
export function mergeAggregation(parent: AggregationConfig, child: AggregationConfig): AggregationConfig {
  return {
    testcases: child.testcases ?? parent.testcases,
    subtasks: child.subtasks ?? parent.subtasks,
  }
}

/**
 * 校验同级列表中的 scoreRatio 总和是否超过 1。
 */
export function validateSiblingRatios(items: unknown[], label: string, ctx: ValidationContext): void {
  validateSiblingRatioEntries(items.map((value, index) => ({ value, index })), label, ctx)
}

/**
 * 校验带索引的 scoreRatio 条目，忽略未声明比例的条目并累加显式比例。
 */
export function validateSiblingRatioEntries(items: IndexedValue[], label: string, ctx: ValidationContext): void {
  const explicitRatios: DecimalUnits[] = []

  items.forEach(({ value: item, index }) => {
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
    explicitRatios.push(decimalUnitsFromNumber(item.scoreRatio))
  })

  if (decimalUnitsExceedOne(explicitRatios)) {
    ctx.errors.push(`${label} explicit scoreRatio values must not sum above 1.`)
  }
}

/**
 * 将 JS number 转为十进制整数和缩放位数，支持科学计数法输入。
 */
function decimalUnitsFromNumber(value: number): DecimalUnits {
  const [coefficient, exponentPart] = value.toString().toLowerCase().split('e')
  const exponent = exponentPart === undefined ? 0 : Number(exponentPart)
  const [wholePart, fractionPart = ''] = coefficient.split('.')
  const digits = `${wholePart}${fractionPart}` || '0'
  const scale = fractionPart.length - exponent

  if (scale <= 0) {
    return { units: BigInt(digits) * pow10(-scale), scale: 0 }
  }
  return { units: BigInt(digits), scale }
}

/**
 * 在统一小数位后判断多个十进制比例之和是否超过 1。
 */
function decimalUnitsExceedOne(values: DecimalUnits[]): boolean {
  const scale = Math.max(0, ...values.map((value) => value.scale))
  const sum = values.reduce((total, value) => total + value.units * pow10(scale - value.scale), 0n)
  return sum > pow10(scale)
}

/**
 * 返回 10 的 BigInt 幂，用于十进制比例缩放。
 */
function pow10(exponent: number): bigint {
  return 10n ** BigInt(exponent)
}
