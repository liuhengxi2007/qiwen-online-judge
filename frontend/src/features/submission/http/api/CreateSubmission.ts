import type { CreateSubmissionRequest } from '@/features/submission/model/request/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/features/submission/model/response/SubmissionDetail'
import {
  fromSubmissionDetailContract,
  toCreateSubmissionRequestContract,
} from '@/features/submission/http/codec/SubmissionHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  return postJson(
    '/api/submissions',
    fromSubmissionDetailContract,
    toCreateSubmissionRequestContract(request),
  )
}
