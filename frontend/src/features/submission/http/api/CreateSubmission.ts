import type {
  CreateSubmissionRequest,
  SubmissionDetail,
} from '@/features/submission/domain/submission'
import {
  fromSubmissionDetailContract,
  toCreateSubmissionRequestContract,
} from '@/features/submission/domain/submission'
import { postJson } from '@/shared/api/http-client'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  return postJson(
    '/api/submissions',
    fromSubmissionDetailContract,
    toCreateSubmissionRequestContract(request),
  )
}
