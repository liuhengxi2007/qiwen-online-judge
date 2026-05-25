import type { SubmissionDetail } from '@/features/submission/model/response/SubmissionDetail'
import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import { submissionIdValue } from '@/features/submission/lib/submission-parsers'
import { fromSubmissionDetailContract } from '@/features/submission/http/codec/SubmissionHttpCodecs'
import { requestJson } from '@/shared/api/http-client'

export async function getSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return requestJson(`/api/submissions/${submissionIdValue(submissionId)}`, fromSubmissionDetailContract)
}
