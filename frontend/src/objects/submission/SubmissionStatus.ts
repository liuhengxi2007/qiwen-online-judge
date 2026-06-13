/** 提交状态枚举；表示队列、运行、完成或系统失败阶段。 */
export type SubmissionStatus = 'queued' | 'running' | 'completed' | 'failed'

const supportedSubmissionStatuses = ['queued', 'running', 'completed', 'failed'] as const satisfies readonly SubmissionStatus[]

/** 判断字符串是否为受支持提交状态。 */
export function isSubmissionStatus(value: string): value is SubmissionStatus {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedSubmissionStatuses.includes(value as SubmissionStatus)
}

/** 判断提交状态是否已经不会继续推进；用于轮询停止条件。 */
export function isTerminalSubmissionStatus(status: SubmissionStatus): boolean {
  switch (status) {
    case 'completed':
    case 'failed':
      return true
    case 'queued':
    case 'running':
      return false
  }
}
