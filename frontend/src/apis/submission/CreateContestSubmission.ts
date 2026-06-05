import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class CreateContestSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: CreateSubmissionRequest

  constructor(contestSlug: ContestSlug, request: CreateSubmissionRequest) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/submissions`
    this.request = request
  }

  body(): CreateSubmissionRequest {
    return this.request
  }
}
