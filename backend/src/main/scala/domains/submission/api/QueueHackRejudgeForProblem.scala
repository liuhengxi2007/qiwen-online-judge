package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.ProblemId
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.response.SuccessResponse

import java.sql.Connection

object QueueHackRejudgeForProblem extends InternalOnlyApi[ProblemId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge/queue-hack-rejudge")

  override def plan(connection: Connection, problemId: ProblemId): IO[SuccessResponse] =
    SubmissionJudgeTable
      .queueHackRejudgeForProblem(connection, problemId)
      .as(SuccessResponse(code = None, message = None, params = Map.empty))
