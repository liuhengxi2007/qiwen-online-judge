import type { SubmissionId } from '@/objects/submission/SubmissionId'

export type CreateHackRequest = {
  targetSubmissionId: SubmissionId
  subtaskIndex: number
  input: string
  strategyProviderSource: string | null
}
