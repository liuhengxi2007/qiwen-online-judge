import type { SubmissionDetail } from '@/features/submission/model/response/SubmissionDetail'
import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import { submissionIdValue } from '@/features/submission/lib/submission-parsers'
import { fromSubmissionDetailContract } from '@/features/submission/http/codec/SubmissionHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function rejudgeSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/rejudge`, fromSubmissionDetailContract, {})
}
