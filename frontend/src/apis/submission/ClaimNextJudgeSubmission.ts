import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { ClaimedSubmission } from '@/objects/submission/ClaimedSubmission'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'

type ClaimNextJudgeSubmissionBody = {
  languages: SubmissionLanguage[]
  runningState: SubmissionJudgeState
  minPriority: number
}

export class ClaimNextJudgeSubmission implements APIMessage<ClaimedSubmission | null> {
  declare readonly responseType?: ClaimedSubmission | null
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/claim-next'
  private readonly languages: SubmissionLanguage[]
  private readonly runningState: SubmissionJudgeState
  private readonly minPriority: number

  constructor(languages: SubmissionLanguage[], runningState: SubmissionJudgeState, minPriority: number) {
    this.languages = languages
    this.runningState = runningState
    this.minPriority = minPriority
  }

  body(): ClaimNextJudgeSubmissionBody {
    return { languages: this.languages, runningState: this.runningState, minPriority: this.minPriority }
  }
}
