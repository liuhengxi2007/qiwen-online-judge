import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'

export class CreateSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath = 'submissions'
  private readonly request: CreateSubmissionRequest

  constructor(request: CreateSubmissionRequest) {
    this.request = request
  }

  body(): CreateSubmissionRequest {
    return this.request
  }
}

export type CreateSubmissionMultipartProgram = {
  role: string
  language: SubmissionLanguage
  sourcePart: string
}

export type CreateSubmissionMultipartSource = {
  sourcePart: string
  source: string | File
}

export type CreateSubmissionMultipartRequest = {
  problemSlug: ProblemSlug
  programs: CreateSubmissionMultipartProgram[]
  sources: CreateSubmissionMultipartSource[]
}

export class CreateSubmissionMultipart implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath = 'submissions'
  private readonly request: CreateSubmissionMultipartRequest

  constructor(request: CreateSubmissionMultipartRequest) {
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
