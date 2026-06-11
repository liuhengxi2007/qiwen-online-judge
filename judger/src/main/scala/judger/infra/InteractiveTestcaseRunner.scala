package judger.infra

import cats.effect.IO
import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTask, JudgeTaskTestcase, JudgeTaskTool, JudgeTestcaseResult}
import judger.config.AppConfig
import judger.infra.JudgeTestcaseResults.*
import judger.objects.{ProcessResult, RuntimeCommand}

import java.nio.file.Path

object InteractiveTestcaseRunner:
  private[infra] def run(
    task: JudgeTask,
    config: AppConfig,
    subtask: judgeprotocol.objects.response.JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    roleCommands: Map[String, RuntimeCommand],
    interactor: JudgeTaskTool,
    interactorCommand: RuntimeCommand,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeTestcaseResult] =
    strategyProviderRuntime(testcase, tools) match
      case Left(_) =>
        IO.pure(testcaseAcceptedByProtocol(testcase))
      case Right(strategyProvider) =>
        runWithStrategy(
          task = task,
          config = config,
          subtask = subtask,
          testcase = testcase,
          workingDirectory = workingDirectory,
          sandbox = sandbox,
          input = input,
          answerBytes = answerBytes,
          roleCommands = roleCommands,
          interactor = interactor,
          interactorCommand = interactorCommand,
          tools = tools,
          strategyProvider = strategyProvider
        )

  private[infra] def runWithStrategy(
    task: JudgeTask,
    config: AppConfig,
    subtask: judgeprotocol.objects.response.JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    roleCommands: Map[String, RuntimeCommand],
    interactor: JudgeTaskTool,
    interactorCommand: RuntimeCommand,
    tools: JudgeToolPreparation.PreparedTools,
    strategyProvider: Option[StrategyProviderRuntime]
  ): IO[JudgeTestcaseResult] =
    InteractiveProcessRunner
      .run(config, subtask, testcase, input, workingDirectory, sandbox, roleCommands, interactor, interactorCommand, strategyProvider)
      .flatMap {
        case Left(reason) =>
          IO.pure(testcaseSystemError(testcase, reason))
        case Right(runResult) if InteractiveJudgeRunner.interactiveToolCpuLimitExceeded(interactor, strategyProvider.map(_.tool), runResult.interactor, runResult.strategyProvider) =>
          IO.pure(testcaseAcceptedByProtocol(testcase))
        case Right(runResult) =>
          val timeoutProcesses = interactiveProcessesForTimeout(testcase, interactor, strategyProvider, runResult)
          if InteractiveJudgeRunner.interactiveWallOnlyTimeout(timeoutProcesses) then
            InteractiveProcessRunner.readStrategyProviderWaitMs(runResult.strategyProviderReadMonitor, runResult.interactor).map { readWaitMs =>
              InteractiveJudgeRunner.interactiveWallOnlyVerdict(
                participants = runResult.participants,
                participantCpuLimitMs = testcase.limits.timeMs.value.toLong,
                processes = timeoutProcesses,
                fallback = runResult.interactor,
                strategyProviderReadWaitMs = readWaitMs,
                strategyProviderIdleLimitMs = runResult.strategyProviderReadMonitor.map(_.idleLimitMs)
              ) match
                case Some((SubmissionVerdict.AcceptedByProtocol, _)) =>
                  testcaseAcceptedByProtocol(testcase)
                case Some((verdict, processResult)) =>
                  testcaseResult(testcase, BigDecimal(0), verdict, None, None, processResult)
                case None =>
                  testcaseSystemError(testcase, JudgeFailureReason.SystemError)
            }
          else
            scoreInteractiveRun(task, testcase, workingDirectory, sandbox, input, answerBytes, tools, strategyProvider, runResult)
      }

  private def scoreInteractiveRun(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    tools: JudgeToolPreparation.PreparedTools,
    strategyProvider: Option[StrategyProviderRuntime],
    runResult: InteractiveRunResult
  ): IO[JudgeTestcaseResult] =
    if runResult.status.contains("accepted_by_protocol") then
      IO.pure(testcaseAcceptedByProtocol(testcase))
    else if strategyFailed(strategyProvider, runResult.strategyProvider) then
      IO.pure(testcaseAcceptedByProtocol(testcase))
    else if runResult.interactor.exitCode.getOrElse(-1) != 0 then
      IO.pure(testcaseSystemError(testcase, JudgeFailureReason.InteractorRuntimeFailed))
    else
      InteractiveJudgeRunner.participantFailure(runResult.participants, testcase.limits.timeMs.value.toLong) match
        case Some((verdict, participantResult)) =>
          IO.pure(testcaseResult(testcase, BigDecimal(0), verdict, None, None, participantResult))
        case None =>
          CheckerRunner.score(task, testcase, workingDirectory, sandbox, input, runResult.output, answerBytes, tools.checkers).map {
            case Right(checkerScore) =>
              testcaseResult(
                testcase,
                checkerScore.score,
                if checkerScore.score == BigDecimal(1) then SubmissionVerdict.Accepted else SubmissionVerdict.WrongAnswer,
                checkerScore.message,
                None,
                runResult.interactor.copy(stdout = runResult.output)
              )
            case Left(reason) =>
              testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), runResult.interactor)
          }

  private[infra] def strategyProviderRuntime(
    testcase: JudgeTaskTestcase,
    tools: JudgeToolPreparation.PreparedTools
  ): Either[Unit, Option[StrategyProviderRuntime]] =
    testcase.strategyProvider match
      case None => Right(None)
      case Some(provider) if provider.limits.isEmpty =>
        Left(())
      case Some(provider) if tools.failedStrategyProviders.contains(provider.source.path) =>
        Left(())
      case Some(provider) =>
        tools.strategyProviders.get(provider.source.path) match
          case None => Left(())
          case Some(command) => Right(Some(StrategyProviderRuntime(provider, command)))

  private def interactiveProcessesForTimeout(
    testcase: JudgeTaskTestcase,
    interactor: JudgeTaskTool,
    strategyProvider: Option[StrategyProviderRuntime],
    runResult: InteractiveRunResult
  ): List[(ProcessResult, Long)] =
    val participantProcesses =
      runResult.participants.map { case (_, result) => result -> testcase.limits.timeMs.value.toLong }
    val interactorProcess =
      interactor.limits.toList.map(limits => runResult.interactor -> limits.timeMs.value.toLong)
    val strategyProcess =
      strategyProvider.toList.flatMap(provider =>
        provider.tool.limits.toList.zip(runResult.strategyProvider.toList).map { case (limits, result) =>
          result -> limits.timeMs.value.toLong
        }
      )
    participantProcesses ++ interactorProcess ++ strategyProcess

  private def strategyFailed(strategyProvider: Option[StrategyProviderRuntime], result: Option[ProcessResult]): Boolean =
    strategyProvider.exists(_ =>
      result match
        case None => true
        case Some(current) if current.timedOut => false
        case Some(current) => current.exitCode.getOrElse(-1) != 0
    )
