import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { fromSubmissionDetailContract } from '@/apis/submission/codecs/SubmissionHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function rejudgeSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/rejudge`, fromSubmissionDetailContract, {})
}
