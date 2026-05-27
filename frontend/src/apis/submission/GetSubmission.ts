import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { fromSubmissionDetailContract } from '@/apis/submission/codecs/SubmissionHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function getSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return requestJson(`/api/submissions/${submissionIdValue(submissionId)}`, fromSubmissionDetailContract)
}
