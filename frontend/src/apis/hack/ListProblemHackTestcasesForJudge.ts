import type { APIMessage } from '@/system/api/api-message'
import type { ProblemId } from '@/objects/problem/ProblemId'

export class ListProblemHackTestcasesForJudge implements APIMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/testcases'
  private readonly problemId: ProblemId

  constructor(problemId: ProblemId) {
    this.problemId = problemId
  }

  body(): ProblemId {
    return this.problemId
  }
}
