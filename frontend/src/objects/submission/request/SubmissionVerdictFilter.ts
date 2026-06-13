import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'

/** 提交列表 verdict 过滤器；all/pending 为列表筛选特殊值。 */
export type SubmissionVerdictFilter = 'all' | 'pending' | SubmissionVerdict

const supportedSubmissionVerdictFilters = [
  'all',
  'pending',
  'accepted',
  'accepted_by_protocol',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'idleness_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]

/** 判断字符串是否为受支持提交 verdict 过滤值。 */
export function isSubmissionVerdictFilter(value: string): value is SubmissionVerdictFilter {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionVerdictFilters.includes(value as SubmissionVerdictFilter)
}
