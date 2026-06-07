import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateHackRequest } from '@/objects/hack/request/CreateHackRequest'
import type { HackDetail } from '@/objects/hack/response/HackDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'

export class CreateHack implements APIWithSessionMessage<HackDetail> {
  declare readonly responseType?: HackDetail
  readonly method = 'POST'
  readonly apiPath = 'hacks'
  private readonly request: CreateHackRequest

  constructor(request: CreateHackRequest) {
    this.request = request
  }

  body(): CreateHackRequest {
    return this.request
  }
}

export type CreateHackMultipartRequest = {
  targetSubmissionId: SubmissionId
  subtaskIndex: number
  input: { kind: 'text'; value: string } | { kind: 'file'; value: File }
  strategyProviderSource?: { kind: 'text'; value: string } | { kind: 'file'; value: File } | null
}

export class CreateHackMultipart implements APIWithSessionMessage<HackDetail> {
  declare readonly responseType?: HackDetail
  readonly method = 'POST'
  readonly apiPath = 'hacks'
  private readonly request: CreateHackMultipartRequest

  constructor(request: CreateHackMultipartRequest) {
    this.request = request
  }

  body(): undefined {
    return undefined
  }

  formData(): FormData {
    const formData = new FormData()
    formData.set('targetSubmissionId', String(submissionIdValue(this.request.targetSubmissionId)))
    formData.set('subtaskIndex', String(this.request.subtaskIndex))
    if (this.request.input.kind === 'file') {
      formData.set('inputFile', this.request.input.value)
    } else {
      formData.set('inputText', this.request.input.value)
    }

    const strategyProviderSource = this.request.strategyProviderSource
    if (strategyProviderSource?.kind === 'file') {
      formData.set('strategyProviderFile', strategyProviderSource.value)
    } else if (strategyProviderSource?.kind === 'text') {
      formData.set('strategyProviderSource', strategyProviderSource.value)
    }

    return formData
  }
}
