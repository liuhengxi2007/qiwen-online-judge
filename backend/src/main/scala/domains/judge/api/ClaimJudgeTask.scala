package domains.judge.api

import cats.effect.IO
import domains.auth.api.PublicResponseApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTaskBuilder
import domains.judge.utils.JudgeTokenAuth
import domains.judger.api.GetActiveJudgerSupportedLanguages
import domains.hack.api.{ClaimNextHackAttempt, ClaimedHackAttempt, ListProblemHackTestcasesForJudge}
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.ProblemDataManifest
import domains.problem.utils.ProblemDataStorage
import domains.submission.api.{ClaimNextJudgeSubmission, UpdateSubmissionJudgeState}
import domains.submission.objects.SubmissionStatus
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeCompletion, SubmissionJudgeState}
import domains.submission.utils.SubmissionJudgeRules
import domains.submission.utils.SubmissionProgramStorage
import io.circe.syntax.*
import judgeprotocol.objects.SubmissionLanguage
import judgeprotocol.objects.request.ClaimJudgeTaskRequest
import judgeprotocol.objects.response.{HackTask, JudgeFailureReason, JudgeResult, JudgeTask, JudgeWorkerTask}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class ClaimJudgeTask(
  judgeConfig: JudgeConfig,
  problemDataStorage: ProblemDataStorage,
  submissionProgramStorage: SubmissionProgramStorage
) extends PublicResponseApi[ClaimJudgeTaskRequest]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/claim")

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
          buildJudgeTask(connection, claimedSubmission).flatMap {
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
                oldScore = claimedHack.oldResult.subtasks.find(_.index == claimedHack.subtaskIndex).map(_.lowestScore).getOrElse(BigDecimal(0)),
                newScore = None,
                newResult = None,
                validatorMessage = None,
                standardMessage = None,
                targetMessage = Some(error.message)
              )
              domains.hack.api.RecordHackAttemptResult
                .plan(connection, domains.hack.api.RecordHackAttemptResult.input(claimedHack.hackId, request))
                .as(None)
            case Right(task) =>
              IO.pure(Some(taskResponse(JudgeWorkerTask.hack(task))))
          }
    yield response

  private def buildJudgeTask(
    connection: Connection,
    claimedSubmission: ClaimedSubmission
  ): IO[Either[JudgeTaskBuilder.BuildError, JudgeTask]] =
    judgeTaskManifest(connection, claimedSubmission).flatMap {
      case None =>
        IO.pure(Left(JudgeTaskBuilder.BuildError("Problem not found for claimed submission.", JudgeFailureReason.JudgeTaskBuildFailed)))
      case Some(manifest) =>
        loadGeneratedHackTestcases(connection, claimedSubmission).flatMap(hackTestcases => loadConfig(claimedSubmission, manifest, hackTestcases))
    }

  private def buildHackTask(
    connection: Connection,
    claimedHack: ClaimedHackAttempt
  ): IO[Either[JudgeTaskBuilder.BuildError, HackTask]] =
    judgeTaskManifest(connection, claimedHack.targetSubmission).flatMap {
      case None =>
        IO.pure(Left(JudgeTaskBuilder.BuildError("Problem not found for claimed hack attempt.", JudgeFailureReason.JudgeTaskBuildFailed)))
      case Some(manifest) =>
        loadGeneratedHackTestcases(connection, claimedHack.targetSubmission)
          .flatMap(hackTestcases => loadConfig(claimedHack.targetSubmission, manifest, hackTestcases))
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
    hackTestcases: List[JudgeTaskBuilder.GeneratedHackTestcase]
  ): IO[Either[JudgeTaskBuilder.BuildError, JudgeTask]] =
    ProblemDataPath.parse("judge.yaml") match
      case Left(message) =>
        IO.pure(Left(JudgeTaskBuilder.BuildError(message, JudgeFailureReason.JudgeTaskBuildFailed)))
      case Right(configPath) =>
        problemDataStorage.readPath(claimedSubmission.problemSlug, configPath).map {
          case None =>
            Left(JudgeTaskBuilder.BuildError("judge.yaml is required at the problem data root.", JudgeFailureReason.JudgeTaskBuildFailed))
          case Some((_, bytes)) =>
            Right(bytes)
        }.flatMap {
          case Left(message) => IO.pure(Left(message))
          case Right(bytes) =>
            submissionProgramStorage.readSources(claimedSubmission.programManifest).map {
              case Left(message) => Left(JudgeTaskBuilder.BuildError(message, JudgeFailureReason.JudgeTaskBuildFailed))
              case Right(sourceCodes) => JudgeTaskBuilder.buildJudgeTask(bytes, claimedSubmission, sourceCodes, manifest, hackTestcases)
            }
        }

  private def loadGeneratedHackTestcases(
    connection: Connection,
    claimedSubmission: ClaimedSubmission
  ): IO[List[JudgeTaskBuilder.GeneratedHackTestcase]] =
    ListProblemHackTestcasesForJudge.plan(connection, claimedSubmission.problemId).map {
      _.map(testcase =>
        JudgeTaskBuilder.GeneratedHackTestcase(
          subtaskIndex = testcase.subtaskIndex,
          label = Some(s"hack #${testcase.hackId.value}"),
          input = testcase.inputRef,
          answer = testcase.answerRef
        )
      )
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
        SubmissionJudgeCompletion(
          status = SubmissionStatus.Failed,
          judgeResult = Some(systemErrorJudgeResult(reason))
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

  private def taskResponse(task: JudgeWorkerTask): Response[IO] =
    Response[IO](status = Status.Ok).withEntity(task.asJson)

  private def systemErrorJudgeResult(reason: JudgeFailureReason): JudgeResult =
    JudgeResult(
      score = BigDecimal(0),
      lowestScore = BigDecimal(0),
      verdict = judgeprotocol.objects.SubmissionVerdict.SystemError,
      reason = Some(reason),
      timeUsedMs = None,
      memoryUsedKb = None,
      subtasks = Nil,
      baseResult = None
    )
