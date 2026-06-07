import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { APIWithSessionMessage } from '@/system/api/api-message'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { CreateSubmissionMultipartRequest } from './CreateSubmission'

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

export class CreateContestSubmissionMultipart implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: CreateSubmissionMultipartRequest

  constructor(contestSlug: ContestSlug, request: CreateSubmissionMultipartRequest) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/submissions`
    this.request = request
  }

  body(): undefined {
    return undefined
  }

  formData(): FormData {
    const formData = new FormData()
    formData.set('problemSlug', problemSlugValue(this.request.problemSlug))
    formData.set('programs', JSON.stringify(this.request.programs))
    for (const source of this.request.sources) {
      formData.set(source.sourcePart, source.source)
    }
    return formData
  }
}
