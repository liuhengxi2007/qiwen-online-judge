import type { JudgeConfigValidationResult, ValidationContext } from './objects/JudgeConfigValidation'

/**
 * 根据上下文错误集合构造最终校验结果；当前校验器暂不产生 warning。
 */
export function toResult(ctx: ValidationContext): JudgeConfigValidationResult {
  return ctx.errors.length > 0 ? { ok: false, errors: ctx.errors, warnings: [] } : { ok: true, warnings: [] }
}
