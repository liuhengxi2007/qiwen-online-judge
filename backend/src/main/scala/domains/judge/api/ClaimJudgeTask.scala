package domains.judge.api

import cats.effect.IO
import domains.auth.api.PublicResponseApi
import domains.judger.api.GetActiveJudgerSupportedLanguages
import domains.hack.api.{ClaimNextHackAttempt, RecordHackAttemptResult}
import domains.hack.objects.internal.ClaimedHackAttempt
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.ProblemDataManifest
import domains.problem.api.{ProblemDataStorage, ProblemDataStorageContext}
import domains.submission.api.{ClaimNextJudgeSubmission, UpdateSubmissionJudgeState}
import domains.submission.objects.SubmissionStatus
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeState}
import domains.submission.api.SubmissionJudgeRules
import domains.submission.api.{SubmissionProgramStorage, SubmissionProgramStorageContext}
import io.circe.syntax.*
import judgeprotocol.objects.SubmissionLanguage
import judgeprotocol.objects.request.ClaimJudgeTaskRequest
import judgeprotocol.objects.response.{HackTask, JudgeFailureReason, JudgeResult, JudgeResultSummary, JudgeTask, JudgeWorkerTask}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** judge worker 领取任务的公开 API；使用共享 token 认证，优先领取普通提交，其次 hack，再领取低优先级提交。API 对齐例外：worker 通道只供 judger 调用，不提供站点前端 wrapper。 */
final case class ClaimJudgeTask(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorageContext,
  submissionProgramStorage: SubmissionProgramStorageContext
) extends PublicResponseApi[ClaimJudgeTaskRequest]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/claim")

  /** 校验 worker token 并解析 claim 请求；路径参数无业务含义。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ClaimJudgeTaskRequest] =
    val _ = pathParams
    JudgeTokenAuth.ensureJudgeToken(request, judgeConfig) *> request.as[ClaimJudgeTaskRequest]

  /** 校验 judger 注册和心跳有效性后返回 judge/hack 任务；没有任务时返回 204。 */
  override def plan(connection: Connection, request: ClaimJudgeTaskRequest): IO[Response[IO]] =
    for
      claimedAt <- IO.realTimeInstant.map(instant => Instant.ofEpochMilli(instant.toEpochMilli))
      maybeSupportedLanguages <- GetActiveJudgerSupportedLanguages.plan(
        connection,
        GetActiveJudgerSupportedLanguages.input(request.judgerId, judgeConfig.heartbeatTimeoutMs)
      )
      response <- maybeSupportedLanguages match
        case None =>
          HttpApiError.raise(HttpApiError.badRequest(s"Judger ${request.judgerId.value} is not registered or its lease expired."))
        case Some(supportedLanguages) =>
          claimTask(connection, supportedLanguages, claimedAt)
    yield response

  private def claimTask(
    connection: Connection,
    supportedLanguages: List[SubmissionLanguage],
    claimedAt: Instant
  ): IO[Response[IO]] =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, claimedAt) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(runningState) =>
        val domainLanguages = supportedLanguages.flatMap(SubmissionJudgeRules.toSubmissionLanguage)
        claimJudgeSubmissionTask(connection, domainLanguages, runningState, claimedAt, minPriority = 0).flatMap {
          case Some(response) => IO.pure(response)
          case None =>
            claimHackTask(connection, domainLanguages).flatMap {
              case Some(response) => IO.pure(response)
              case None =>
                claimJudgeSubmissionTask(connection, domainLanguages, runningState, claimedAt, minPriority = Int.MinValue).map {
                  case Some(response) => response
                  case None => Response[IO](status = Status.NoContent)
                }
            }
        }

  private def claimJudgeSubmissionTask(
    connection: Connection,
    supportedLanguages: List[domains.submission.objects.SubmissionLanguage],
    runningState: SubmissionJudgeState,
    claimedAt: Instant,
    minPriority: Int
  ): IO[Option[Response[IO]]] =
    ClaimNextJudgeSubmission
      .plan(
        connection,
        ClaimNextJudgeSubmission.input(supportedLanguages, runningState, minPriority)
      )
      .flatMap {
        case None =>
          IO.pure(None)
        case Some(claimedSubmission) =>
          buildJudgeTask(connection, claimedSubmission, claimedAt).flatMap {
            case Left(error) =>
              failClaimedJudgeTask(connection, claimedSubmission, claimedAt, error.reason).flatMap {
                case Left(lifecycleMessage) => HttpApiError.raise(HttpApiError.badRequest(lifecycleMessage))
                case Right(_) => HttpApiError.raise(HttpApiError.badRequest(error.message))
              }
            case Right(task) =>
              IO.pure(Some(taskResponse(JudgeWorkerTask.judge(task))))
          }
      }

  private def claimHackTask(
    connection: Connection,
    supportedLanguages: List[domains.submission.objects.SubmissionLanguage]
  ): IO[Option[Response[IO]]] =
    for
      claimedAt <- IO.realTimeInstant
      maybeClaimedHack <- ClaimNextHackAttempt.plan(connection, ClaimNextHackAttempt.input(supportedLanguages, claimedAt))
      response <- maybeClaimedHack match
        case None => IO.pure(None)
        case Some(claimedHack) =>
          buildHackTask(connection, claimedHack).flatMap {
            case Left(error) =>
              val request = judgeprotocol.objects.request.ReportHackResultRequest(
                status = "failed",
                answer = None,
                newResult = None,
                validatorMessage = None,
                standardMessage = None,
                targetMessage = Some(error.message)
              )
              RecordHackAttemptResult(problemDataStorage)
                .plan(connection, RecordHackAttemptResult.input(claimedHack.hackId, request))
                .as(None)
            case Right(task) =>
              IO.pure(Some(taskResponse(JudgeWorkerTask.hack(task))))
          }
    yield response

  private def buildJudgeTask(
    connection: Connection,
    claimedSubmission: ClaimedSubmission,
    claimedAt: Instant
  ): IO[Either[JudgeTaskBuilder.BuildError, JudgeTask]] =
    judgeTaskManifest(connection, claimedSubmission).flatMap {
      case None =>
        IO.pure(Left(JudgeTaskBuilder.BuildError("Problem not found for claimed submission.", JudgeFailureReason.JudgeTaskBuildFailed)))
      case Some(manifest) =>
        loadConfig(claimedSubmission, manifest, claimedAt.toEpochMilli)
    }

  private def buildHackTask(
    connection: Connection,
    claimedHack: ClaimedHackAttempt
  ): IO[Either[JudgeTaskBuilder.BuildError, HackTask]] =
    judgeTaskManifest(connection, claimedHack.targetSubmission).flatMap {
      case None =>
        IO.pure(Left(JudgeTaskBuilder.BuildError("Problem not found for claimed hack attempt.", JudgeFailureReason.JudgeTaskBuildFailed)))
      case Some(manifest) =>
        loadConfig(claimedHack.targetSubmission, manifest, startedAtEpochMilli = 0L)
          .map(_.map(task =>
            HackTask(
              hackId = claimedHack.hackId.value,
              targetTask = task,
              subtaskIndex = claimedHack.subtaskIndex,
              input = claimedHack.input,
              strategyProviderSource = claimedHack.strategyProviderSource,
              oldResult = claimedHack.oldResult
            )
          ))
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
    manifest: ProblemDataManifest,
    startedAtEpochMilli: Long
  ): IO[Either[JudgeTaskBuilder.BuildError, JudgeTask]] =
    ProblemDataPath.parse("judge.yaml") match
      case Left(message) =>
        IO.pure(Left(JudgeTaskBuilder.BuildError(message, JudgeFailureReason.JudgeTaskBuildFailed)))
      case Right(configPath) =>
        ProblemDataStorage.readPath(problemDataStorage, claimedSubmission.problemSlug, configPath).map {
          case None =>
            Left(JudgeTaskBuilder.BuildError("judge.yaml is required at the problem data root.", JudgeFailureReason.JudgeTaskBuildFailed))
          case Some((_, bytes)) =>
            Right(bytes)
        }.flatMap {
          case Left(message) => IO.pure(Left(message))
          case Right(bytes) =>
            SubmissionProgramStorage.readSources(submissionProgramStorage, claimedSubmission.programManifest).map {
              case Left(message) => Left(JudgeTaskBuilder.BuildError(message, JudgeFailureReason.JudgeTaskBuildFailed))
              case Right(sourceCodes) => JudgeTaskBuilder.buildJudgeTask(bytes, claimedSubmission, sourceCodes, manifest, startedAtEpochMilli)
            }
        }

  private def failClaimedJudgeTask(
    connection: Connection,
    claimedSubmission: ClaimedSubmission,
    claimedAt: Instant,
    reason: JudgeFailureReason
  ): IO[Either[String, Unit]] =
    SubmissionJudgeRules.beginJudging(SubmissionJudgeState.queued, claimedAt).flatMap { runningState =>
      SubmissionJudgeRules.completeJudging(
        runningState,
        SubmissionStatus.Failed,
        Some(systemErrorJudgeResult(reason)),
        claimedAt
      )
    } match
      case Left(lifecycleMessage) =>
        IO.pure(Left(lifecycleMessage))
      case Right(failedState) =>
        UpdateSubmissionJudgeState
          .plan(connection, UpdateSubmissionJudgeState.input(claimedSubmission.id, failedState))
          .as(Right(()))

  private def taskResponse(task: JudgeWorkerTask): Response[IO] =
    Response[IO](status = Status.Ok).withEntity(task.asJson)

  private def systemErrorJudgeResult(reason: JudgeFailureReason): JudgeResult =
    JudgeResult(
      baseResult = JudgeResultSummary.failed(reason),
      worstResult = JudgeResultSummary.failed(reason),
      subtasks = Nil
    )
