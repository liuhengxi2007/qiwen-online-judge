import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import { submissionIdValue } from '@/features/submission/lib/submission-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/model/response/SuccessResponse'

export async function deleteSubmission(submissionId: SubmissionId): Promise<SuccessResponse> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/delete`, decodeSuccessResponse, {})
}
