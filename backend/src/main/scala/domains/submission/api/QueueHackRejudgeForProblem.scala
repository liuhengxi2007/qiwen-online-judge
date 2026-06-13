package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.ProblemId
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 内部 hack 重判排队 API；成功 hack 物化后把该题已完成提交低优先级重新入队。 */
object QueueHackRejudgeForProblem extends InternalOnlyApi[ProblemId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/queue-hack-rejudge")

  /** 对题目下已终态提交排队重判，输出通用成功响应。 */
  override def plan(connection: Connection, problemId: ProblemId): IO[SuccessResponse] =
    SubmissionJudgeTable
      .queueHackRejudgeForProblem(connection, problemId)
      .as(SuccessResponse(code = None, message = None, params = Map.empty))
