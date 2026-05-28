package domains.judge.http.api

import cats.effect.IO
import domains.auth.http.PublicResponseApi
import domains.judge.application.JudgeConfig
import domains.judge.application.JudgeTaskBuilder
import domains.judge.http.JudgeApiSupport
import domains.judger.table.judger.JudgerTable
import domains.problem.application.ProblemDataStorage
import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeCompletion, SubmissionJudgeState}
import domains.submission.rules.SubmissionJudgeRules
import domains.submission.table.submission.SubmissionJudgeTable
import io.circe.syntax.*
import judgeprotocol.objects.{ClaimJudgeTaskRequest, JudgeTask, JudgerId}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.http.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class ClaimJudgeTask(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage
) extends PublicResponseApi[ClaimJudgeTaskRequest]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/judge/claim")

  override def decode(request: Request[IO], pathParams: PathParams): IO[ClaimJudgeTaskRequest] =
    val _ = pathParams
    JudgeApiSupport.ensureJudgeToken(request, judgeConfig) *> request.as[ClaimJudgeTaskRequest]

  override def plan(connection: Connection, request: ClaimJudgeTaskRequest): IO[Response[IO]] =
    for
      claimedAt <- IO.realTimeInstant
      maybeSupportedLanguages <- JudgerTable.findActiveSupportedLanguages(connection, request.judgerId, judgeConfig.heartbeatTimeoutMs)
      response <- maybeSupportedLanguages match
        case None =>
          HttpApiError.raise(HttpApiError.badRequest(s"Judger ${request.judgerId.value} is not registered or its lease expired."))
        case Some(supportedLanguages) =>
          claimTask(connection, request.judgerId, supportedLanguages, claimedAt)
    yield response

  private def claimTask(
    connection: Connection,
    judgerId: JudgerId,
    supportedLanguages: List[judgeprotocol.objects.SubmissionLanguage],
    claimedAt: Instant
  ): IO[Response[IO]] =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, claimedAt) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(runningState) =>
        SubmissionJudgeTable
          .claimNextForLanguages(connection, supportedLanguages.flatMap(SubmissionJudgeRules.toSubmissionLanguage), runningState)
          .flatMap {
            case None =>
              IO.pure(Response[IO](status = Status.NoContent))
            case Some(claimedSubmission) =>
              JudgeTaskBuilder.buildJudgeTask(connection, problemDataStorage, claimedSubmission).flatMap {
                case Left(message) =>
                  failClaimedJudgeTask(connection, claimedSubmission, judgerId, claimedAt, message).flatMap {
                    case Left(lifecycleMessage) => HttpApiError.raise(HttpApiError.badRequest(lifecycleMessage))
                    case Right(_) => HttpApiError.raise(HttpApiError.badRequest(message))
                  }
                case Right(task) =>
                  IO.pure(taskResponse(task))
              }
          }

  private def failClaimedJudgeTask(
    connection: Connection,
    claimedSubmission: ClaimedSubmission,
    judgerId: JudgerId,
    claimedAt: Instant,
    message: String
  ): IO[Either[String, Unit]] =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, claimedAt).flatMap { runningState =>
      SubmissionJudgeRules.completeJudging(
        runningState,
        SubmissionJudgeCompletion(
          status = SubmissionStatus.Failed,
          verdict = Some(SubmissionVerdict.SystemError),
          judgeMessage = Some(s"${judgerId.value}: $message"),
          timeUsedMs = None,
          memoryUsedKb = None,
          score = None,
          judgeResult = None
        ),
        claimedAt
      )
    } match
      case Left(lifecycleMessage) =>
        IO.pure(Left(lifecycleMessage))
      case Right(failedState) =>
        SubmissionJudgeTable.updateJudgeState(connection, claimedSubmission.id, failedState).as(Right(()))

  private def taskResponse(task: JudgeTask): Response[IO] =
    Response[IO](status = Status.Ok).withEntity(task.asJson)
