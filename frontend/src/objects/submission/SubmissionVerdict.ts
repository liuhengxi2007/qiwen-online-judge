/** 提交评测 verdict 枚举；表示用户代码的最终判定结果。 */
export type SubmissionVerdict =
  | 'accepted'
  | 'accepted_by_protocol'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'idleness_limit_exceeded'
  | 'system_error'

const supportedSubmissionVerdicts = [
  'accepted',
  'accepted_by_protocol',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'idleness_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdict[]

/** 判断字符串是否为受支持提交 verdict。 */
export function isSubmissionVerdict(value: string): value is SubmissionVerdict {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionVerdicts.includes(value as SubmissionVerdict)
}
