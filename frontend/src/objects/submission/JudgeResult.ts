import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'
import { isJudgeFailureReason } from '@/objects/submission/JudgeFailureReason'
import type { JudgeTestcaseResult } from '@/objects/submission/JudgeTestcaseResult'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import { isSubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

/** 完整判题结果；包含基础结果、最差结果和所有子任务明细。 */
export type JudgeResult = {
  baseResult: JudgeResultSummary
  worstResult: JudgeResultSummary
  subtasks: JudgeSubtaskResult[]
}

/** 解码不可信 JSON 为 JudgeResult；内部 API worker 载荷需要在边界处校验结果树结构。 */
export function decodeJudgeResult(value: unknown): JudgeResult {
  if (!isRecord(value) || !Array.isArray(value.subtasks)) {
    throw new Error('Invalid judge result payload.')
  }

  return {
    baseResult: decodeJudgeResultSummary(value.baseResult),
    worstResult: decodeJudgeResultSummary(value.worstResult),
    subtasks: value.subtasks.map(decodeJudgeSubtaskResult),
  }
}

function decodeJudgeResultSummary(value: unknown): JudgeResultSummary {
  if (!isRecord(value)) {
    throw new Error('Invalid judge result summary.')
  }

  return {
    score: numberField(value.score, 'score'),
    verdict: decodeSubmissionVerdict(value.verdict),
    reason: decodeNullableStringEnum(value.reason, isJudgeFailureReason, 'judge failure reason'),
    timeUsedMs: nullableIntegerField(value.timeUsedMs, 'timeUsedMs'),
    memoryUsedKb: nullableIntegerField(value.memoryUsedKb, 'memoryUsedKb'),
  }
}

function decodeJudgeSubtaskResult(value: unknown): JudgeSubtaskResult {
  if (!isRecord(value) || !Array.isArray(value.testcases)) {
    throw new Error('Invalid judge subtask result.')
  }

  return {
    index: integerField(value.index, 'index'),
    label: nullableStringField(value.label, 'label'),
    baseResult: decodeJudgeResultSummary(value.baseResult),
    worstResult: decodeJudgeResultSummary(value.worstResult),
    testcases: value.testcases.map(decodeJudgeTestcaseResult),
  }
}

function decodeJudgeTestcaseResult(value: unknown): JudgeTestcaseResult {
  if (!isRecord(value)) {
    throw new Error('Invalid judge testcase result.')
  }

  return {
    index: integerField(value.index, 'index'),
    label: nullableStringField(value.label, 'label'),
    testcaseType: decodeTestcaseType(value.testcaseType),
    score: numberField(value.score, 'score'),
    verdict: decodeSubmissionVerdict(value.verdict),
    message: nullableStringField(value.message, 'message'),
    reason: decodeNullableStringEnum(value.reason, isJudgeFailureReason, 'judge failure reason'),
    timeUsedMs: nullableIntegerField(value.timeUsedMs, 'timeUsedMs'),
    memoryUsedKb: nullableIntegerField(value.memoryUsedKb, 'memoryUsedKb'),
  }
}

function decodeSubmissionVerdict(value: unknown): SubmissionVerdict {
  if (typeof value === 'string' && isSubmissionVerdict(value)) {
    return value
  }
  throw new Error('Invalid submission verdict.')
}

function decodeTestcaseType(value: unknown): JudgeTestcaseResult['testcaseType'] {
  if (value === 'main' || value === 'sample' || value === 'hack') {
    return value
  }
  throw new Error('Invalid judge testcase type.')
}

function decodeNullableStringEnum<T extends string>(
  value: unknown,
  isSupported: (raw: string) => raw is T,
  fieldName: string,
): T | null {
  if (value === null) {
    return null
  }
  if (typeof value === 'string' && isSupported(value)) {
    return value
  }
  throw new Error(`Invalid ${fieldName}.`)
}

function nullableStringField(value: unknown, fieldName: string): string | null {
  if (value === null || typeof value === 'string') {
    return value
  }
  throw new Error(`Invalid judge result ${fieldName}.`)
}

function numberField(value: unknown, fieldName: string): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  throw new Error(`Invalid judge result ${fieldName}.`)
}

function integerField(value: unknown, fieldName: string): number {
  if (typeof value === 'number' && Number.isSafeInteger(value)) {
    return value
  }
  throw new Error(`Invalid judge result ${fieldName}.`)
}

function nullableIntegerField(value: unknown, fieldName: string): number | null {
  if (value === null) {
    return null
  }
  return integerField(value, fieldName)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
