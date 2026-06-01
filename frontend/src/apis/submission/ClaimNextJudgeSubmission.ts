import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { ClaimedSubmission } from '@/objects/submission/ClaimedSubmission'
import type { SubmissionJudgeState } from '@/objects/submission/SubmissionJudgeState'
import { readNullable } from '@/objects/shared/PageResponse'
import { fromClaimedSubmissionContract } from '@/objects/submission/ClaimedSubmission'

type ClaimNextJudgeSubmissionBody = {
  languages: SubmissionLanguage[]
  runningState: SubmissionJudgeState
}

export class ClaimNextJudgeSubmission implements APIMessage<ClaimedSubmission | null> {
  declare readonly responseType?: ClaimedSubmission | null
  readonly method = 'POST'
  readonly decode = (value: unknown) => readNullable(value, 'claimed submission', fromClaimedSubmissionContract)
  readonly apiPath = 'internal/submissions/judge/claim-next'
  private readonly languages: SubmissionLanguage[]
  private readonly runningState: SubmissionJudgeState

  constructor(languages: SubmissionLanguage[], runningState: SubmissionJudgeState) {
    this.languages = languages
    this.runningState = runningState
  }

  body(): ClaimNextJudgeSubmissionBody {
    return { languages: this.languages, runningState: this.runningState }
  }
}
