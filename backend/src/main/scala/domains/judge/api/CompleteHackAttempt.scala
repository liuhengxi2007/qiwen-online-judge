package domains.judge.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.PublicApi
import domains.hack.api.RecordHackAttemptResult
import domains.hack.objects.HackId
import domains.judge.utils.{JudgeConfig, JudgeTokenAuth}
import domains.problem.utils.ProblemDataStorage
import domains.submission.api.QueueHackRejudgeForProblem
import io.circe.Encoder
import judgeprotocol.objects.request.ReportHackResultRequest
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class CompleteHackAttempt(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
) extends PublicApi[(HackId, ReportHackResultRequest), SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/hacks/:hackId/complete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(HackId, ReportHackResultRequest)] =
    for
      _ <- JudgeTokenAuth.ensureJudgeToken(request, judgeConfig)
      hackId <- HttpApiError.fromEitherBadRequest(pathParams.require("hackId").flatMap(HackId.parse))
      resultRequest <- request.as[ReportHackResultRequest]
    yield hackId -> resultRequest

  override def plan(connection: Connection, input: (HackId, ReportHackResultRequest)): IO[SuccessResponse] =
    val (hackId, request) = input
    for
      maybeProblemId <- RecordHackAttemptResult(problemDataStorage).plan(connection, RecordHackAttemptResult.input(hackId, request))
      _ <- maybeProblemId.traverse_(problemId => QueueHackRejudgeForProblem.plan(connection, problemId))
    yield SuccessResponse(code = None, message = None, params = Map.empty)
