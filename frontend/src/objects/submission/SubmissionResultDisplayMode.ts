/** 提交结果展示模式；决定详情页优先显示 verdict 还是分数。 */
export type SubmissionResultDisplayMode = 'verdict' | 'score'

const supportedSubmissionResultDisplayModes = [
  'verdict',
  'score',
] as const satisfies readonly SubmissionResultDisplayMode[]

/** 判断字符串是否为受支持的提交结果展示模式。 */
export function isSubmissionResultDisplayMode(value: string): value is SubmissionResultDisplayMode {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionResultDisplayModes.includes(value as SubmissionResultDisplayMode)
}
