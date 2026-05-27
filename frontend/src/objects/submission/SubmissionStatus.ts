export type SubmissionStatus = 'queued' | 'running' | 'completed' | 'failed'

const supportedSubmissionStatuses = ['queued', 'running', 'completed', 'failed'] as const satisfies readonly SubmissionStatus[]

export function isSubmissionStatus(value: string): value is SubmissionStatus {
  return supportedSubmissionStatuses.includes(value as SubmissionStatus)
}

export function fromSubmissionStatusContract(value: SubmissionStatus): SubmissionStatus {
  return value
}

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
