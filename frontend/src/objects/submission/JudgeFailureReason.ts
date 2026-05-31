export type JudgeFailureReason =
  | 'judge_task_build_failed'
  | 'judger_runtime_failed'
  | 'checker_compile_failed'
  | 'checker_runtime_failed'
  | 'problem_data_load_failed'
  | 'system_error'

const supportedJudgeFailureReasons = [
  'judge_task_build_failed',
  'judger_runtime_failed',
  'checker_compile_failed',
  'checker_runtime_failed',
  'problem_data_load_failed',
  'system_error',
] as const satisfies readonly JudgeFailureReason[]

export function isJudgeFailureReason(value: string): value is JudgeFailureReason {
  return supportedJudgeFailureReasons.includes(value as JudgeFailureReason)
}
