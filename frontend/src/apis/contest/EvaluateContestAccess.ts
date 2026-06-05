import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemId } from '@/objects/problem/ProblemId'

type EvaluateContestAccessBody = {
  contestSlug: ContestSlug
  problemId: ProblemId | null
}

export class EvaluateContestAccess implements APIWithSessionMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/contests/evaluate-access'
  private readonly contestSlug: ContestSlug
  private readonly problemId: ProblemId | null

  constructor(contestSlug: ContestSlug, problemId: ProblemId | null) {
    this.contestSlug = contestSlug
    this.problemId = problemId
  }

  body(): EvaluateContestAccessBody {
    return {
      contestSlug: this.contestSlug,
      problemId: this.problemId,
    }
  }
}
