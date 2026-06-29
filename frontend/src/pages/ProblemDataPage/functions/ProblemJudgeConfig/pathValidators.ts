import { parseProblemDataPath, problemDataPathValue } from '@/objects/problem/ProblemDataPath'

import type { ValidationContext } from './objects/JudgeConfigValidation'

/**
 * 校验必填文件引用，要求非空字符串且路径存在于题目数据树。
 */
export function validateRequiredFileRef(value: unknown, label: string, ctx: ValidationContext): void {
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} is required.`)
    return
  }
  validateExistingFile(value, label, ctx)
}

/**
 * 校验可选文件引用；缺失时跳过，存在时要求非空且路径有效。
 */
export function validateOptionalFileRef(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} must be a nonempty string when present.`)
    return
  }
  validateExistingFile(value, label, ctx)
}

/**
 * 校验路径字段，返回规范化后的题目数据路径或记录错误。
 */
export function validatePathValue(value: unknown, label: string, ctx: ValidationContext): string | null {
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} is required.`)
    return null
  }
  return validateExistingFile(value, label, ctx)
}

/**
 * 校验文件路径语法并确认文件存在于当前数据树。
 */
export function validateExistingFile(rawPath: string, label: string, ctx: ValidationContext): string | null {
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
