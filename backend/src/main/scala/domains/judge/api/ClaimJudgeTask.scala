package domains.judge.api

import cats.effect.IO
import domains.auth.api.PublicResponseApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTaskBuilder
import domains.judge.utils.JudgeTokenAuth
import domains.judger.api.GetActiveJudgerSupportedLanguages
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.ProblemDataManifest
import domains.problem.utils.ProblemDataStorage
import domains.submission.api.{ClaimNextJudgeSubmission, UpdateSubmissionJudgeState}
import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeCompletion, SubmissionJudgeState}
import domains.submission.utils.SubmissionJudgeRules
import io.circe.syntax.*
import judgeprotocol.objects.{JudgerId, SubmissionLanguage}
import judgeprotocol.objects.request.ClaimJudgeTaskRequest
import judgeprotocol.objects.response.JudgeTask
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

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
    JudgeTokenAuth.ensureJudgeToken(request, judgeConfig) *> request.as[ClaimJudgeTaskRequest]

  override def plan(connection: Connection, request: ClaimJudgeTaskRequest): IO[Response[IO]] =
    for
      claimedAt <- IO.realTimeInstant
      maybeSupportedLanguages <- GetActiveJudgerSupportedLanguages.plan(
        connection,
        GetActiveJudgerSupportedLanguages.input(request.judgerId, judgeConfig.heartbeatTimeoutMs)
      )
      response <- maybeSupportedLanguages match
        case None =>
          HttpApiError.raise(HttpApiError.badRequest(s"Judger ${request.judgerId.value} is not registered or its lease expired."))
        case Some(supportedLanguages) =>
          claimTask(connection, request.judgerId, supportedLanguages, claimedAt)
    yield response

  private def claimTask(
    connection: Connection,
    judgerId: JudgerId,
    supportedLanguages: List[SubmissionLanguage],
    claimedAt: Instant
  ): IO[Response[IO]] =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, claimedAt) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(runningState) =>
        ClaimNextJudgeSubmission
          .plan(
            connection,
            ClaimNextJudgeSubmission.input(supportedLanguages.flatMap(SubmissionJudgeRules.toSubmissionLanguage), runningState)
          )
          .flatMap {
            case None =>
              IO.pure(Response[IO](status = Status.NoContent))
            case Some(claimedSubmission) =>
              buildJudgeTask(connection, claimedSubmission).flatMap {
                case Left(message) =>
                  failClaimedJudgeTask(connection, claimedSubmission, judgerId, claimedAt, message).flatMap {
                    case Left(lifecycleMessage) => HttpApiError.raise(HttpApiError.badRequest(lifecycleMessage))
                    case Right(_) => HttpApiError.raise(HttpApiError.badRequest(message))
                  }
                case Right(task) =>
                  IO.pure(taskResponse(task))
              }
          }

  private def buildJudgeTask(
    connection: Connection,
    claimedSubmission: ClaimedSubmission
  ): IO[Either[String, JudgeTask]] =
    judgeTaskManifest(connection, claimedSubmission).flatMap {
      case None =>
        IO.pure(Left("Problem not found for claimed submission."))
      case Some(manifest) =>
        loadConfig(claimedSubmission, manifest)
    }

  private def judgeTaskManifest(
    connection: Connection,
    claimedSubmission: ClaimedSubmission
  ): IO[Option[ProblemDataManifest]] =
    GetJudgeProblemDataManifest.plan(
      connection,
      GetJudgeProblemDataManifest.input(claimedSubmission.problemId, claimedSubmission.problemSlug)
    )

  private def loadConfig(
    claimedSubmission: ClaimedSubmission,
    manifest: ProblemDataManifest
  ): IO[Either[String, JudgeTask]] =
    ProblemDataPath.parse("judge.yaml") match
      case Left(message) =>
        IO.pure(Left(message))
      case Right(configPath) =>
        problemDataStorage.readPath(claimedSubmission.problemSlug, configPath).map {
          case None =>
            Left("judge.yaml is required at the problem data root.")
          case Some((_, bytes)) =>
            JudgeTaskBuilder.buildJudgeTask(bytes, claimedSubmission, manifest)
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
        UpdateSubmissionJudgeState
          .plan(connection, UpdateSubmissionJudgeState.input(claimedSubmission.id, failedState))
          .as(Right(()))

  private def taskResponse(task: JudgeTask): Response[IO] =
    Response[IO](status = Status.Ok).withEntity(task.asJson)
