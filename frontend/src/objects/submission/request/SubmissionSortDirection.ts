/** 提交列表排序方向；asc 为升序，desc 为降序。 */
export type SubmissionSortDirection = 'asc' | 'desc'

const supportedSubmissionSortDirections = ['asc', 'desc'] as const satisfies readonly SubmissionSortDirection[]

/** 判断字符串是否为受支持提交排序方向。 */
export function isSubmissionSortDirection(value: string): value is SubmissionSortDirection {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionSortDirections.includes(value as SubmissionSortDirection)
}
