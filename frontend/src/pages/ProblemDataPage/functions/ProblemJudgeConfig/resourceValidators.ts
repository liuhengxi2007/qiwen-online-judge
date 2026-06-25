import type {
  CheckerConfig,
  LimitsConfig,
  StandardConfig,
  ToolConfig,
  ToolLimitsConfig,
  ValidationContext,
} from './types'
import { isRecord, validateIntegerRange, validatePathValue } from './utils'

/**
 * 校验 limits 对象；未声明返回 null，声明时要求 timeMs 和 memoryMb 都在允许范围内。
 */
export function validateLimits(value: unknown, label: string, ctx: ValidationContext): LimitsConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return null
  }

  const timeMs = validateIntegerRange({
    value: value.timeMs,
    label: `${label}.timeMs`,
    range: { min: 1, max: 600000 },
    ctx,
  })
  const memoryMb = validateIntegerRange({
    value: value.memoryMb,
    label: `${label}.memoryMb`,
    range: { min: 1, max: 65536 },
    ctx,
  })
  return timeMs !== null && memoryMb !== null ? { timeMs, memoryMb } : null
}

/**
 * 校验 checker 配置；支持内置 checker 和 cpp17/cpp 自定义 checker 文件路径。
 */
export function validateChecker(value: unknown, label: string, ctx: ValidationContext): CheckerConfig | null {
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

/**
 * 校验不带资源限制的工具配置；允许简写为路径字符串或对象 path 字段。
 */
export function validateTool(value: unknown, label: string, ctx: ValidationContext): ToolConfig | null {
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

/**
 * 校验带资源限制的工具配置；必须声明 path 和 limits。
 */
export function validateLimitedTool(value: unknown, label: string, ctx: ValidationContext): ToolConfig | null {
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

/**
 * 校验 standard 配置；undefined 表示继承父级，false 表示禁用，对象表示答案生成器。
 */
export function validateStandard(value: unknown, label: string, ctx: ValidationContext): StandardConfig {
  if (value === undefined) {
    return { type: 'unspecified' }
  }
  if (value === false) {
    return { type: 'none' }
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object or false.`)
    return { type: 'unspecified' }
  }

  if (value.language !== 'cpp17' && value.language !== 'python3') {
    ctx.errors.push(`${label}.language must be cpp17 or python3.`)
  }
  const path = validatePathValue(value.path, `${label}.path`, ctx)
  return path ? { type: 'generator', path } : { type: 'unspecified' }
}

/**
 * 计算 standard 继承结果；子级未声明时使用父级配置。
 */
export function inheritStandard(value: StandardConfig, parent: StandardConfig): StandardConfig {
  return value.type === 'unspecified' ? parent : value
}

/**
 * 校验 Hack 开启时的必要依赖，要求 validator 和 standard 策略都明确存在。
 */
export function validateHackCapability(input: {
  label: string
  enabled: boolean
  dependencies: {
    validator: ToolConfig | null
    standard: StandardConfig
  }
  ctx: ValidationContext
}): void {
  if (!input.enabled) {
    return
  }
  if (!input.dependencies.validator) {
    input.ctx.errors.push(`Validator is required for ${input.label} when hack is enabled.`)
  }
  if (input.dependencies.standard.type === 'unspecified') {
    input.ctx.errors.push(`standard must be declared as an answer generator object or false for ${input.label} when hack is enabled.`)
  }
}

/**
 * 校验工具 limits 对象；工具限制必须同时声明合法的 timeMs 和 memoryMb。
 */
function validateToolLimits(value: unknown, label: string, ctx: ValidationContext): ToolLimitsConfig | null {
  if (!isRecord(value)) {
    ctx.errors.push(`${label} is required and must be an object.`)
    return null
  }

  const timeMs = validateIntegerRange({
    value: value.timeMs,
    label: `${label}.timeMs`,
    range: { min: 1, max: 600000 },
    ctx,
  })
  const memoryMb = validateIntegerRange({
    value: value.memoryMb,
    label: `${label}.memoryMb`,
    range: { min: 1, max: 65536 },
    ctx,
  })
  return timeMs !== null && memoryMb !== null ? { timeMs, memoryMb } : null
}
