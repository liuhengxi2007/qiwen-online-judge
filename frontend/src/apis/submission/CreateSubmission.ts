import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import {
  fromSubmissionDetailContract,
  toCreateSubmissionRequestContract,
} from '@/apis/submission/codecs/SubmissionHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function createSubmission(request: CreateSubmissionRequest): Promise<SubmissionDetail> {
  return postJson(
    '/api/submissions',
    fromSubmissionDetailContract,
    toCreateSubmissionRequestContract(request),
  )
}
