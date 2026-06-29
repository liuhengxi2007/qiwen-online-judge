/** 提交列表排序字段；与后端排序参数一一对应。 */
export type SubmissionSort = 'submitted' | 'time' | 'memory' | 'code_length'

const supportedSubmissionSorts = ['submitted', 'time', 'memory', 'code_length'] as const satisfies readonly SubmissionSort[]

/** 判断字符串是否为受支持提交排序字段。 */
export function isSubmissionSort(value: string): value is SubmissionSort {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionSorts.includes(value as SubmissionSort)
}
