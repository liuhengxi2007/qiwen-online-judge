export type SubmissionStatus = 'queued' | 'running' | 'completed' | 'failed'

const supportedSubmissionStatuses = ['queued', 'running', 'completed', 'failed'] as const satisfies readonly SubmissionStatus[]

export function isSubmissionStatus(value: string): value is SubmissionStatus {
  return supportedSubmissionStatuses.includes(value as SubmissionStatus)
}

export function fromSubmissionStatusContract(value: unknown): SubmissionStatus {
  if (typeof value !== 'string' || !isSubmissionStatus(value)) {
    throw new Error('Invalid submission status in contract payload.')
  }

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
