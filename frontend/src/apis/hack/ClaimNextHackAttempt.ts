import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'

type ClaimNextHackAttemptBody = {
  languages: SubmissionLanguage[]
  startedAt: string
}

export class ClaimNextHackAttempt implements APIMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/claim-next'
  private readonly languages: SubmissionLanguage[]
  private readonly startedAt: string

  constructor(languages: SubmissionLanguage[], startedAt: string) {
    this.languages = languages
    this.startedAt = startedAt
  }

  body(): ClaimNextHackAttemptBody {
    return { languages: this.languages, startedAt: this.startedAt }
  }
}
