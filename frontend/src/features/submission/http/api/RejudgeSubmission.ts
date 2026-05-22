import type {
  SubmissionDetail,
  SubmissionId,
} from '@/features/submission/domain/submission'
import { submissionIdValue } from '@/features/submission/domain/submission'
import { fromSubmissionDetailContract } from '@/features/submission/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function rejudgeSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/rejudge`, fromSubmissionDetailContract, {})
}
