import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemId } from '@/objects/problem/ProblemId'

/** 内部比赛访问评估请求体；problemId 为空表示只评估比赛本身访问。 */
type EvaluateContestAccessBody = {
  contestSlug: ContestSlug
  problemId: ProblemId | null
}

/** 评估当前会话对比赛/比赛题目的访问；输出类型尚未建模，权限由后端判断。 */
/** FIXME-CN: 后端返回 Option[EvaluateContestAccess.Result]，这里用 unknown 会让调用方失去权限字段类型保护，应补前端镜像类型。 */
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
