import type {
  CreateSubmissionRequest,
  SubmissionDetail,
} from '@/features/submission/domain/submission'
import {
  fromSubmissionDetailContract,
  toCreateSubmissionRequestContract,
} from '@/features/submission/domain/submission'
import { postJson } from '@/shared/api/http-client'
import type { CreateSubmissionRequest as CreateSubmissionRequestContract } from '@contracts/submission'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  return postJson(
    '/api/submissions',
    fromSubmissionDetailContract,
    toCreateSubmissionRequestContract(request) satisfies CreateSubmissionRequestContract,
  )
}
