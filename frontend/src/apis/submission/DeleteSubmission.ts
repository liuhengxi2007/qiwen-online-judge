import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/submission-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export async function deleteSubmission(submissionId: SubmissionId): Promise<SuccessResponse> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/delete`, decodeSuccessResponse, {})
}
