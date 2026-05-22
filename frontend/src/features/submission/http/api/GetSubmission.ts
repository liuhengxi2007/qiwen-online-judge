import type {
  SubmissionDetail,
  SubmissionId,
} from '@/features/submission/domain/submission'
import {
  fromSubmissionDetailContract,
  submissionIdValue,
} from '@/features/submission/domain/submission'
import { requestJson } from '@/shared/api/http-client'

export async function getSubmission(submissionId: SubmissionId): Promise<SubmissionDetail> {
  return requestJson(`/api/submissions/${submissionIdValue(submissionId)}`, fromSubmissionDetailContract)
}
