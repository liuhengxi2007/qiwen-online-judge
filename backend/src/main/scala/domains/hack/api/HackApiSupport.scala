package domains.hack.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.response.{HackSubtaskAvailability, HackSubtaskInfo, SubmissionHackAvailability}
import domains.judge.utils.JudgeTaskBuilder
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.objects.ProblemDataPath
import domains.problem.utils.ProblemDataStorage
import domains.submission.api.GetSubmission
import domains.submission.objects.SubmissionStatus
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import domains.submission.utils.SubmissionProgramStorage
import judgeprotocol.objects.response.{JudgeTask, JudgeTaskHackConfig, JudgeTaskSubtask}
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection
import java.util.UUID

object HackApiSupport:

  val MaxHackInputChars: Int = 10 * 1024 * 1024
  val MaxStrategyProviderChars: Int = 200000

  final case class TargetContext(
    submission: domains.submission.objects.response.SubmissionDetail,
    task: JudgeTask,
    subtask: JudgeTaskSubtask
  )

  final case class TargetTaskContext(
    submission: domains.submission.objects.response.SubmissionDetail,
    task: JudgeTask
  )

  def loadTargetContext(
    connection: Connection,
    actor: AuthenticatedUser,
    submissionId: domains.submission.objects.SubmissionId,
    subtaskIndex: Int,
    submissionProgramStorage: SubmissionProgramStorage,
    problemDataStorage: ProblemDataStorage
  ): IO[TargetContext] =
    for
      target <- loadTargetTask(connection, actor, submissionId, submissionProgramStorage, problemDataStorage)
      subtask <- target.task.subtasks.find(_.index == subtaskIndex) match
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.badRequest("Hack target subtask is unavailable for the current judge configuration."))
      targetWorstScore = targetSubtaskWorstScore(target.submission, subtask.index)
      _ <- HttpApiError.ensure(subtask.hack.enabled, HttpApiError.badRequest("Hack is disabled for this subtask."))
      _ <- HttpApiError.ensure(targetWorstScore > BigDecimal(0), HttpApiError.badRequest("This subtask's worst score is already zero."))
      _ <- HttpApiError.ensure(subtask.validator.nonEmpty, HttpApiError.badRequest("This subtask has no validator."))
      _ <- HttpApiError.ensure(
        subtask.hack.answerGeneration != JudgeTaskHackConfig.StandardAnswerGeneration || subtask.standard.nonEmpty,
        HttpApiError.badRequest("This subtask has no answer generator.")
      )
    yield TargetContext(target.submission, target.task, subtask)

  def loadTargetTask(
    connection: Connection,
    actor: AuthenticatedUser,
    submissionId: domains.submission.objects.SubmissionId,
    submissionProgramStorage: SubmissionProgramStorage,
    problemDataStorage: ProblemDataStorage
  ): IO[TargetTaskContext] =
    for
      submission <- GetSubmission(submissionProgramStorage).plan(connection, actor, submissionId)
      _ <- HttpApiError.ensure(submission.status == SubmissionStatus.Completed, HttpApiError.badRequest("Only completed submissions can be hacked."))
      _ <- HttpApiError.ensure(submission.judgeResult.nonEmpty, HttpApiError.badRequest("Target submission has no judge result."))
      task <- buildTask(connection, submission, problemDataStorage)
    yield TargetTaskContext(submission, task)

  def subtaskInfo(context: TargetContext): HackSubtaskInfo =
    val targetWorstScore = targetSubtaskWorstScore(context.submission, context.subtask.index)
    HackSubtaskInfo(
      targetSubmissionId = context.submission.id,
      problemId = context.submission.problemId,
      problemSlug = context.submission.problemSlug,
      problemTitle = context.submission.problemTitle,
      targetSubmitter = context.submission.submitter,
      subtaskIndex = context.subtask.index,
      subtaskLabel = context.subtask.label,
      oldWorstScore = targetWorstScore,
      mode = context.subtask.mode.`type`,
      requiresStrategyProvider = requiresStrategyProvider(context.subtask)
    )

  def hackAvailability(context: TargetTaskContext): SubmissionHackAvailability =
    SubmissionHackAvailability(
      subtasks = context.task.subtasks.map { subtask =>
        val targetWorstScore = targetSubtaskWorstScore(context.submission, subtask.index)
        val reason =
          if !subtask.hack.enabled then Some("hack_disabled")
          else if targetWorstScore <= BigDecimal(0) then Some("score_already_zero")
          else None
        HackSubtaskAvailability(
          subtaskIndex = subtask.index,
          canHack = reason.isEmpty,
          reason = reason
        )
      }
    )

  def targetSubtaskWorstScore(submission: domains.submission.objects.response.SubmissionDetail, subtaskIndex: Int): BigDecimal =
    submission.judgeResult
      .flatMap(_.subtasks.find(_.index == subtaskIndex))
      .map(_.worstResult.score)
      .getOrElse(BigDecimal(0))

  def requiresStrategyProvider(subtask: JudgeTaskSubtask): Boolean =
    subtask.mode.`type` == "interactive" && subtask.testcases.exists(_.strategyProvider.nonEmpty)

  def normalizeHackInput(input: String): String =
    val normalizedNewlines = input.replace("\r\n", "\n").replace('\r', '\n')
    if normalizedNewlines.isEmpty then ""
    else
      val strippedLines = normalizedNewlines
        .split("\n", -1)
        .map(_.replaceAll("[ \t]+$", ""))
        .mkString("\n")
      if strippedLines.endsWith("\n") then strippedLines else s"$strippedLines\n"

  def validateHackText(input: String, strategyProviderSource: Option[String], requiresStrategyProvider: Boolean): IO[Unit] =
    val normalizedStrategy = strategyProviderSource.filter(_.trim.nonEmpty)
    for
      _ <- HttpApiError.ensure(input.length <= MaxHackInputChars, HttpApiError.badRequest(s"Hack input must be at most $MaxHackInputChars characters."))
      _ <- HttpApiError.ensure(
        normalizedStrategy.forall(_.length <= MaxStrategyProviderChars),
        HttpApiError.badRequest(s"Strategy provider source must be at most $MaxStrategyProviderChars characters.")
      )
      _ <- HttpApiError.ensure(
        !requiresStrategyProvider || normalizedStrategy.nonEmpty,
        HttpApiError.badRequest("Strategy provider source is required for this interactive subtask.")
      )
    yield ()

  private def buildTask(
    connection: Connection,
    submission: domains.submission.objects.response.SubmissionDetail,
    problemDataStorage: ProblemDataStorage
  ): IO[JudgeTask] =
    val manifestInput = submission.programs.map { case (role, program) => role -> (program.language -> program.sourceCode) }
    for
      programManifest <- HttpApiError.fromEitherBadRequest(
        SubmissionProgramManifest.fromPrograms(UUID.fromString("00000000-0000-4000-8000-000000000000"), manifestInput)
      )
      manifest <- GetJudgeProblemDataManifest
        .plan(connection, GetJudgeProblemDataManifest.input(submission.problemId, submission.problemSlug))
        .flatMap {
          case Some(value) => IO.pure(value)
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
        }
      configPath <- HttpApiError.fromEitherBadRequest(ProblemDataPath.parse("judge.yaml"))
      configBytes <- problemDataStorage.readPath(submission.problemSlug, configPath).flatMap {
        case Some((_, bytes)) => IO.pure(bytes)
        case None => HttpApiError.raise(HttpApiError.badRequest("judge.yaml is required at the problem data root."))
      }
      claimedSubmission = ClaimedSubmission(
        id = submission.id,
        problemId = submission.problemId,
        problemSlug = submission.problemSlug,
        programManifest = programManifest
      )
      sourceCodes = submission.programs.map { case (role, program) => role -> program.sourceCode }
      task <- JudgeTaskBuilder.buildJudgeTask(configBytes, claimedSubmission, sourceCodes, manifest) match
        case Right(task) => IO.pure(task)
        case Left(error) => HttpApiError.raise(HttpApiError.badRequest(error.message))
    yield task
