import type { SubmissionId } from '@/features/submission/domain/submission'
import { submissionIdValue } from '@/features/submission/domain/submission'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/model/SuccessResponse'

export async function deleteSubmission(submissionId: SubmissionId): Promise<SuccessResponse> {
  return postJson(`/api/submissions/${submissionIdValue(submissionId)}/delete`, decodeSuccessResponse, {})
}
