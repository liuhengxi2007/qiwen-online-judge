package domains.hack.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.HackMode
import domains.hack.objects.response.{HackSubtaskAvailability, HackSubtaskInfo, SubmissionHackAvailability}
import domains.judge.api.JudgeTaskBuilder
import domains.problem.api.GetJudgeProblemDataManifest
import domains.problem.objects.ProblemDataPath
import domains.problem.api.{ProblemDataStorage, ProblemDataStorageContext}
import domains.submission.api.GetSubmission
import domains.submission.objects.SubmissionStatus
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import domains.submission.api.SubmissionProgramStorageContext
import judgeprotocol.objects.response.{JudgeTask, JudgeTaskHackConfig, JudgeTaskSubtask}
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection
import java.util.UUID

/** hack API 共享业务逻辑；负责目标提交加载、任务重建、可 hack 条件和输入文本校验；API 对齐例外：这是多个后端 hack 端点共享的支持代码，不是前端端点。 */
object HackApiSupport:

  val MaxHackInputChars: Int = 10 * 1024 * 1024
  val MaxStrategyProviderChars: Int = 200000

  /** 已校验可 hack 的目标上下文；包含提交详情、构建出的任务和目标子任务。 */
  final case class TargetContext(
    submission: domains.submission.objects.response.SubmissionDetail,
    task: JudgeTask,
    subtask: JudgeTaskSubtask
  )

  /** 目标提交对应的任务上下文；用于可用性列表。 */
  final case class TargetTaskContext(
    submission: domains.submission.objects.response.SubmissionDetail,
    task: JudgeTask
  )

  /** 加载目标提交并校验指定子任务可 hack；失败返回 bad request 或隐藏资源错误。 */
  def loadTargetContext(
    connection: Connection,
    actor: AuthenticatedUser,
    submissionId: domains.submission.objects.SubmissionId,
    subtaskIndex: Int,
    submissionProgramStorage: SubmissionProgramStorageContext,
    problemDataStorage: ProblemDataStorageContext
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

  /** 加载目标提交详情并重建 JudgeTask；要求提交已完成且有 judgeResult。 */
  def loadTargetTask(
    connection: Connection,
    actor: AuthenticatedUser,
    submissionId: domains.submission.objects.SubmissionId,
    submissionProgramStorage: SubmissionProgramStorageContext,
    problemDataStorage: ProblemDataStorageContext
  ): IO[TargetTaskContext] =
    for
      submission <- GetSubmission(submissionProgramStorage).plan(connection, actor, submissionId)
      _ <- HttpApiError.ensure(submission.status == SubmissionStatus.Completed, HttpApiError.badRequest("Only completed submissions can be hacked."))
      _ <- HttpApiError.ensure(submission.judgeResult.nonEmpty, HttpApiError.badRequest("Target submission has no judge result."))
      task <- buildTask(connection, submission, problemDataStorage)
    yield TargetTaskContext(submission, task)

  /** 从可 hack 上下文生成前端展示的子任务信息。 */
  def subtaskInfo(context: TargetContext): IO[HackSubtaskInfo] =
    val targetWorstScore = targetSubtaskWorstScore(context.submission, context.subtask.index)
    HackMode.parse(context.subtask.mode.`type`) match
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(mode) =>
        IO.pure(
          HackSubtaskInfo(
            targetSubmissionId = context.submission.id,
            problemId = context.submission.problemId,
            problemSlug = context.submission.problemSlug,
            problemTitle = context.submission.problemTitle,
            targetSubmitter = context.submission.submitter,
            subtaskIndex = context.subtask.index,
            subtaskLabel = context.subtask.label,
            oldWorstScore = targetWorstScore,
            mode = mode,
            requiresStrategyProvider = requiresStrategyProvider(context.subtask)
          )
        )

  /** 计算目标提交每个子任务的 hack 可用性及不可用原因。 */
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

  /** 从目标提交结果中读取子任务最差分；缺失时按 0 处理以禁止 hack。 */
  def targetSubtaskWorstScore(submission: domains.submission.objects.response.SubmissionDetail, subtaskIndex: Int): BigDecimal =
    submission.judgeResult
      .flatMap(_.subtasks.find(_.index == subtaskIndex))
      .map(_.worstResult.score)
      .getOrElse(BigDecimal(0))

  /** 判断交互式子任务是否需要用户提交策略程序。 */
  def requiresStrategyProvider(subtask: JudgeTaskSubtask): Boolean =
    subtask.mode.`type` == "interactive" && subtask.testcases.exists(_.strategyProvider.nonEmpty)

  /** 规范化 hack 输入换行和行尾空白，确保最终以换行结尾。 */
  def normalizeHackInput(input: String): String =
    val normalizedNewlines = input.replace("\r\n", "\n").replace('\r', '\n')
    if normalizedNewlines.isEmpty then ""
    else
      val strippedLines = normalizedNewlines
        .split("\n", -1)
        .map(_.replaceAll("[ \t]+$", ""))
        .mkString("\n")
      if strippedLines.endsWith("\n") then strippedLines else s"$strippedLines\n"

  /** 校验 hack 输入和策略源码大小，并在需要策略程序时强制提供。 */
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
    problemDataStorage: ProblemDataStorageContext
  ): IO[JudgeTask] =
    val manifestInput = submission.programs.map { case (role, program) => role -> (program.language -> program.sourceCode) }
    for
      programManifest <- HttpApiError.fromEitherBadRequest(
        // 注意：固定 UUID 只用于从提交详情临时重建 JudgeTask，不会作为源码对象 key 写回存储。
        SubmissionProgramManifest.fromPrograms(UUID.fromString("00000000-0000-4000-8000-000000000000"), manifestInput)
      )
      manifest <- GetJudgeProblemDataManifest
        .plan(connection, GetJudgeProblemDataManifest.input(submission.problemId, submission.problemSlug))
        .flatMap {
          case Some(value) => IO.pure(value)
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
        }
      configPath <- HttpApiError.fromEitherBadRequest(ProblemDataPath.parse("judge.yaml"))
      configBytes <- ProblemDataStorage.readPath(problemDataStorage, submission.problemSlug, configPath).flatMap {
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
