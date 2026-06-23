/** 判题失败原因枚举；只描述系统/构建/运行失败，不表示普通评测 verdict。对象对齐例外：该类型镜像 judge-protocol，不对应后端 submission domain object。 */
export type JudgeFailureReason =
  | 'judge_task_build_failed'
  | 'judger_runtime_failed'
  | 'checker_compile_failed'
  | 'checker_runtime_failed'
  | 'interactor_compile_failed'
  | 'interactor_runtime_failed'
  | 'problem_data_load_failed'
  | 'system_error'

const supportedJudgeFailureReasons = [
  'judge_task_build_failed',
  'judger_runtime_failed',
  'checker_compile_failed',
  'checker_runtime_failed',
  'interactor_compile_failed',
  'interactor_runtime_failed',
  'problem_data_load_failed',
  'system_error',
] as const satisfies readonly JudgeFailureReason[]

/** 判断字符串是否为受支持的判题失败原因；用于 API 响应防御性窄化。 */
export function isJudgeFailureReason(value: string): value is JudgeFailureReason {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedJudgeFailureReasons.includes(value as JudgeFailureReason)
}
