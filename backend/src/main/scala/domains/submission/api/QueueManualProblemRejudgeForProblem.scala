package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.ProblemId
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.ApiMessageParam
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 内部手动整题重判排队 API；由题目管理端完成权限校验后调用提交域队列写入。 */
object QueueManualProblemRejudgeForProblem extends InternalOnlyApi[ProblemId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/queue-manual-problem-rejudge")

  /** 对题目下终态提交排队重判，并返回实际排队或提权的提交数量。 */
  override def plan(connection: Connection, problemId: ProblemId): IO[SuccessResponse] =
    SubmissionJudgeTable
      .queueManualRejudgeForProblem(connection, problemId)
      .map(queuedCount =>
        SuccessResponse(
          code = None,
          message = None,
          params = Map("queuedCount" -> ApiMessageParam.IntValue(queuedCount))
        )
      )
