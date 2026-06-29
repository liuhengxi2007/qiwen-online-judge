import type { ValidationContext } from './JudgeConfigValidation'

export type IntegerRange = {
  min: number
  max: number
}

export type IntegerRangeValidationInput = {
  value: unknown
  label: string
  range: IntegerRange
  ctx: ValidationContext
}

/**
 * 普通评测资源限制配置，单位与 judge.yaml 字段保持一致。
 */
export type LimitsConfig = {
  timeMs: number
  memoryMb: number
}

/**
 * 工具程序资源限制配置，用于 interactor、strategyProvider 等受限工具。
 */
export type ToolLimitsConfig = {
  timeMs: number
  memoryMb: number
}
