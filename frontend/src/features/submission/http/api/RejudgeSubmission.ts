import type { SubmissionDetail } from '@/features/submission/http/response/SubmissionDetail'
import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import { submissionIdValue } from '@/features/submission/lib/submission-parsers'
import { fromSubmissionDetailContract } from '@/features/submission/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function rejudgeSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/rejudge`, fromSubmissionDetailContract, {})
}
