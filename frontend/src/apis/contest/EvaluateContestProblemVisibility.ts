import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemId } from '@/objects/problem/ProblemId'

type EvaluateContestProblemVisibilityBody = {
  problemId: ProblemId
  submittedAt: string | null
}

export class EvaluateContestProblemVisibility implements APIWithSessionMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/contests/evaluate-problem-visibility'
  private readonly problemId: ProblemId
  private readonly submittedAt: string | null

  constructor(problemId: ProblemId, submittedAt: string | null) {
    this.problemId = problemId
    this.submittedAt = submittedAt
  }

  body(): EvaluateContestProblemVisibilityBody {
    return {
      problemId: this.problemId,
      submittedAt: this.submittedAt,
    }
  }
}
