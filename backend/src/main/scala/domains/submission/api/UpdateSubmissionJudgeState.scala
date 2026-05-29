package domains.submission.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.submission.objects.SubmissionId
import domains.submission.objects.internal.SubmissionJudgeState
import domains.submission.table.submission.SubmissionJudgeTable
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class UpdateSubmissionJudgeStateInput(
  submissionId: SubmissionId,
  judgeState: SubmissionJudgeState
)

object UpdateSubmissionJudgeState extends InternalOnlyApi[UpdateSubmissionJudgeStateInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/submissions/judge-state/update")

  def input(submissionId: SubmissionId, judgeState: SubmissionJudgeState): UpdateSubmissionJudgeStateInput =
    UpdateSubmissionJudgeStateInput(submissionId = submissionId, judgeState = judgeState)

  override def plan(connection: Connection, input: UpdateSubmissionJudgeStateInput): IO[SuccessResponse] =
    SubmissionJudgeTable
      .updateJudgeState(connection, input.submissionId, input.judgeState)
      .as(SuccessResponse(code = None, message = None, params = Map.empty))
