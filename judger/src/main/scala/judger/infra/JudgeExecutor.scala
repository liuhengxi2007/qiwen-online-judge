package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.*
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.objects.{ProcessResult, RuntimeCommand, SandboxExecutionRequest, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

object JudgeExecutor:
  private final case class PreparedPrograms(
    commands: Map[String, RuntimeCommand],
    compileFailedRoles: Set[String]
  )

  private final case class PreparedTools(
    checkers: Map[JudgeTaskFilePath, RuntimeCommand],
    validators: Map[JudgeTaskFilePath, RuntimeCommand],
    interactors: Map[JudgeTaskFilePath, RuntimeCommand],
    strategyProviders: Map[JudgeTaskFilePath, RuntimeCommand],
    failedStrategyProviders: Set[JudgeTaskFilePath]
  )

  private enum ToolCompileOutcome:
    case Success(command: RuntimeCommand)
    case CompileFailed
    case SystemFailed(reason: JudgeFailureReason)

  def judge(
    task: JudgeTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportJudgeResultRequest] =
    withWorkingDirectory(config.workRoot, "qiwen-judger-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        preparePrograms(task, config, workingDirectory, runtimes).flatMap {
          case Left(result) => IO.pure(result)
          case Right(programs) =>
            prepareTools(task, config, workingDirectory, problemDataCache).flatMap {
              case Left(result) => IO.pure(result)
              case Right(tools) => judgeSubtasks(task, workingDirectory, sandbox, problemDataCache, programs, tools)
            }
        }
      }
    }.handleError { _ =>
      taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)
    }

  private def preparePrograms(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[Either[ReportJudgeResultRequest, PreparedPrograms]] =
    task.programs.toList
      .traverse { case (role, program) =>
        runtimes.get(program.language) match
          case None =>
            IO.pure(Right(role -> None))
          case Some(runtime) =>
            runtime.prepare(role, program.sourceCode, config, workingDirectory).map {
              case Right(command) => Right(role -> Some(command))
              case Left(ProgramPrepareFailure.CompileError) => Right(role -> None)
              case Left(ProgramPrepareFailure.SystemError(reason)) => Left(taskSystemError(task, reason))
            }
      }
      .map { prepared =>
        prepared.collectFirst { case Left(result) => Left(result) }.getOrElse {
          val roleCommands = prepared.collect { case Right((role, Some(command))) => role -> command }.toMap
          val failedRoles = prepared.collect { case Right((role, None)) => role }.toSet
          Right(PreparedPrograms(roleCommands, failedRoles))
        }
      }

  private def prepareTools(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache
  ): IO[Either[ReportJudgeResultRequest, PreparedTools]] =
    val checkerSources = uniqueRefs(task.subtasks.flatMap(_.testcases).flatMap(_.checker.source))
    val validatorSources = uniqueRefs(task.subtasks.flatMap(_.testcases).map(_.validator.source))
    val interactorSources = uniqueRefs(task.subtasks.flatMap(_.mode.interactor))
    val strategyProviderSources = uniqueRefs(task.subtasks.flatMap(_.testcases).flatMap(_.strategyProvider.map(_.source)))

    for
      checkers <- compileRequiredTools(task, config, workingDirectory, problemDataCache, checkerSources, JudgeFailureReason.CheckerCompileFailed)
      validators <- checkers match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileRequiredTools(task, config, workingDirectory, problemDataCache, validatorSources, JudgeFailureReason.ValidatorCompileFailed)
      interactors <- validators match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileRequiredTools(task, config, workingDirectory, problemDataCache, interactorSources, JudgeFailureReason.InteractorCompileFailed)
      strategyProviders <- interactors match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileStrategyProviders(task, config, workingDirectory, problemDataCache, strategyProviderSources)
    yield
      (checkers, validators, interactors, strategyProviders) match
        case (Right(checkerCommands), Right(validatorCommands), Right(interactorCommands), Right((strategyCommands, failedStrategies))) =>
          Right(PreparedTools(checkerCommands, validatorCommands, interactorCommands, strategyCommands, failedStrategies))
        case (Left(result), _, _, _) => Left(result)
        case (_, Left(result), _, _) => Left(result)
        case (_, _, Left(result), _) => Left(result)
        case (_, _, _, Left(result)) => Left(result)

  private def compileRequiredTools(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    sources: List[JudgeTaskFileRef],
    compileFailureReason: JudgeFailureReason
  ): IO[Either[ReportJudgeResultRequest, Map[JudgeTaskFilePath, RuntimeCommand]]] =
    sources.traverse { source =>
      compileCppTool(task, config, workingDirectory, problemDataCache, source).map {
        case ToolCompileOutcome.Success(command) => Right(source.path -> command)
        case ToolCompileOutcome.CompileFailed => Left(taskSystemError(task, compileFailureReason))
        case ToolCompileOutcome.SystemFailed(reason) => Left(taskSystemError(task, reason))
      }
    }.map { compiled =>
      compiled.collectFirst { case Left(result) => Left(result) }.getOrElse(Right(compiled.collect { case Right(entry) => entry }.toMap))
    }

  private def compileStrategyProviders(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    sources: List[JudgeTaskFileRef]
  ): IO[Either[ReportJudgeResultRequest, (Map[JudgeTaskFilePath, RuntimeCommand], Set[JudgeTaskFilePath])]] =
    sources.traverse { source =>
      compileCppTool(task, config, workingDirectory, problemDataCache, source).map {
        case ToolCompileOutcome.Success(command) => Right(source.path -> Some(command))
        case ToolCompileOutcome.CompileFailed => Right(source.path -> None)
        case ToolCompileOutcome.SystemFailed(reason) => Left(taskSystemError(task, reason))
      }
    }.map { compiled =>
      compiled.collectFirst { case Left(result) => Left(result) }.getOrElse {
        val commands = compiled.collect { case Right((path, Some(command))) => path -> command }.toMap
        val failed = compiled.collect { case Right((path, None)) => path }.toSet
        Right(commands -> failed)
      }
    }

  private def compileCppTool(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache,
    sourceRef: JudgeTaskFileRef
  ): IO[ToolCompileOutcome] =
    resolveCompilerPath(config).flatMap {
      case Left(_) => IO.pure(ToolCompileOutcome.CompileFailed)
      case Right(compilerPath) =>
        val sourceName = s"tool-${math.abs(sourceRef.path.value.hashCode)}.cpp"
        val executableName = s"tool-${math.abs(sourceRef.path.value.hashCode)}"
        problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, sourceRef).attempt.flatMap {
          case Left(_) =>
            IO.pure(ToolCompileOutcome.SystemFailed(JudgeFailureReason.ProblemDataLoadFailed))
          case Right(sourceBytes) =>
            for
              _ <- IO.blocking {
                Files.write(workingDirectory.resolve(sourceName), sourceBytes)
                Files.writeString(workingDirectory.resolve("testlib.h"), MinimalTestlibHeader, StandardCharsets.UTF_8)
              }
              compileResult <- runHostProcess(
                command = compilerPath,
                args = List(sourceName, "-o", executableName, "-O2", "-std=c++17", "-I", "."),
                cwd = workingDirectory,
                stdin = None,
                limits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048),
                stdoutName = s".$executableName.compile.stdout",
                stderrName = s".$executableName.compile.stderr"
              )
              result <-
                if compileResult.timedOut || compileResult.exitCode.getOrElse(-1) != 0 then IO.pure(ToolCompileOutcome.CompileFailed)
                else
                  ensureExecutableExists(workingDirectory.resolve(executableName)).attempt.map {
                    case Right(_) => ToolCompileOutcome.Success(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1))
                    case Left(_) => ToolCompileOutcome.CompileFailed
                  }
            yield result
        }
    }

  private def judgeSubtasks(
    task: JudgeTask,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    programs: PreparedPrograms,
    tools: PreparedTools
  ): IO[ReportJudgeResultRequest] =
    task.subtasks.traverse(subtask => judgeSubtask(task, subtask, workingDirectory, sandbox, problemDataCache, programs, tools)).map { subtasks =>
      val result = aggregateTask(task, subtasks)
      ReportJudgeResultRequest(
        status = if containsSystemError(result) then SubmissionStatus.Failed else SubmissionStatus.Completed,
        judgeResult = Some(result)
      )
    }

  private def judgeSubtask(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    programs: PreparedPrograms,
    tools: PreparedTools
  ): IO[JudgeSubtaskResult] =
    subtask.mode.`type` match
      case "traditional" =>
        val role = subtask.mode.role.getOrElse("main")
        programCommand(task, programs, role) match
          case None => IO.pure(subtaskCompileError(subtask))
          case Some(command) =>
            subtask.testcases.traverse { testcase =>
              judgeTraditionalTestcase(task, subtask, testcase, workingDirectory, sandbox, problemDataCache, command, tools)
            }.map(testcases => aggregateSubtask(subtask, testcases))
      case "interactive" =>
        val missingRole = subtask.mode.roles.find(role => programCommand(task, programs, role).isEmpty)
        missingRole match
          case Some(_) => IO.pure(subtaskCompileError(subtask))
          case None =>
            subtask.mode.interactor.flatMap(ref => tools.interactors.get(ref.path)) match
              case None => IO.pure(subtaskSystemError(subtask, JudgeFailureReason.InteractorCompileFailed))
              case Some(interactorCommand) =>
                val roleCommands = subtask.mode.roles.flatMap(role => programCommand(task, programs, role).map(role -> _)).toMap
                subtask.testcases.traverse { testcase =>
                  judgeInteractiveTestcase(task, subtask, testcase, workingDirectory, sandbox, problemDataCache, roleCommands, interactorCommand, tools)
                }.map(testcases => aggregateSubtask(subtask, testcases))
      case _ =>
        IO.pure(subtaskSystemError(subtask, JudgeFailureReason.SystemError))

  private def programCommand(task: JudgeTask, programs: PreparedPrograms, role: String): Option[RuntimeCommand] =
    if !task.programs.contains(role) || programs.compileFailedRoles.contains(role) then None
    else programs.commands.get(role)

  private def judgeTraditionalTestcase(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    command: RuntimeCommand,
    tools: PreparedTools
  ): IO[JudgeTestcaseResult] =
    loadTestcaseData(task, testcase, problemDataCache).flatMap {
      case Left(reason) =>
        IO.pure(testcaseSystemError(testcase, reason))
      case Right((input, answerBytes)) =>
        validateInput(testcase, input, workingDirectory, sandbox, tools).flatMap {
          case Left(reason) => IO.pure(testcaseSystemError(testcase, reason))
          case Right(_) =>
            sandbox.run(
              SandboxExecutionRequest(
                phase = s"run-${subtask.index}-${testcase.index}",
                command = command.command,
                args = command.args,
                stdin = Some(input),
                limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
                processLimit = command.processLimit
              ),
              workingDirectory
            ).flatMap(runResult => scoreParticipantRun(task, testcase, workingDirectory, sandbox, input, runResult, answerBytes, tools))
        }
    }

  private def judgeInteractiveTestcase(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    roleCommands: Map[String, RuntimeCommand],
    interactorCommand: RuntimeCommand,
    tools: PreparedTools
  ): IO[JudgeTestcaseResult] =
    loadTestcaseData(task, testcase, problemDataCache).flatMap {
      case Left(reason) =>
        IO.pure(testcaseSystemError(testcase, reason))
      case Right((input, answerBytes)) =>
        validateInput(testcase, input, workingDirectory, sandbox, tools).flatMap {
          case Left(reason) => IO.pure(testcaseSystemError(testcase, reason))
          case Right(_) =>
            runStrategyProvider(testcase, input, workingDirectory, sandbox, tools).flatMap {
              case Left(_) =>
                IO.pure(testcaseAcceptedByProtocol(testcase))
              case Right(strategyOutput) =>
                runInteractor(subtask, testcase, input, strategyOutput, workingDirectory, sandbox, roleCommands, interactorCommand).flatMap {
                  case Left(reason) =>
                    IO.pure(testcaseSystemError(testcase, reason))
                  case Right(runResult) =>
                    val output = runResult.stdout
                    scoreWithChecker(task, testcase, workingDirectory, sandbox, input, output, answerBytes, tools.checkers).map {
                      case Right(checkerScore) =>
                        testcaseResult(
                          testcase,
                          checkerScore.score,
                          if checkerScore.score == BigDecimal(1) then SubmissionVerdict.Accepted else SubmissionVerdict.WrongAnswer,
                          checkerScore.message,
                          None,
                          runResult
                        )
                      case Left(reason) =>
                        testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), runResult)
                    }
                }
            }
        }
    }

  private def scoreParticipantRun(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    input: Array[Byte],
    runResult: ProcessResult,
    answerBytes: Option[Array[Byte]],
    tools: PreparedTools
  ): IO[JudgeTestcaseResult] =
    if runResult.timedOut then
      IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.TimeLimitExceeded, None, None, runResult))
    else if runResult.exitCode.getOrElse(-1) != 0 then
      IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.RuntimeError, None, None, runResult))
    else
      scoreWithChecker(task, testcase, workingDirectory, sandbox, input, runResult.stdout, answerBytes, tools.checkers).map {
        case Right(checkerScore) =>
          testcaseResult(
            testcase,
            checkerScore.score,
            if checkerScore.score == BigDecimal(1) then SubmissionVerdict.Accepted else SubmissionVerdict.WrongAnswer,
            checkerScore.message,
            None,
            runResult
          )
        case Left(reason) =>
          testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), runResult)
      }

  private def validateInput(
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    tools: PreparedTools
  ): IO[Either[JudgeFailureReason, Unit]] =
    tools.validators.get(testcase.validator.source.path) match
      case None => IO.pure(Left(JudgeFailureReason.ValidatorCompileFailed))
      case Some(command) =>
        sandbox.run(
          SandboxExecutionRequest(
            phase = s"validator-${testcase.index}",
            command = command.command,
            args = command.args,
            stdin = Some(input),
            limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
            processLimit = command.processLimit
          ),
          workingDirectory
        ).map { result =>
          if result.timedOut || result.exitCode.getOrElse(-1) != 0 then Left(JudgeFailureReason.TestcaseDataInvalid)
          else Right(())
        }

  private def runStrategyProvider(
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    tools: PreparedTools
  ): IO[Either[Unit, Option[String]]] =
    testcase.strategyProvider match
      case None => IO.pure(Right(None))
      case Some(provider) if tools.failedStrategyProviders.contains(provider.source.path) =>
        IO.pure(Left(()))
      case Some(provider) =>
        tools.strategyProviders.get(provider.source.path) match
          case None => IO.pure(Left(()))
          case Some(command) =>
            sandbox.run(
              SandboxExecutionRequest(
                phase = s"strategy-${testcase.index}",
                command = command.command,
                args = command.args,
                stdin = Some(input),
                limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
                processLimit = command.processLimit
              ),
              workingDirectory
            ).map { result =>
              if result.timedOut || result.exitCode.getOrElse(-1) != 0 then Left(())
              else Right(Some(result.stdout))
            }

  private def runInteractor(
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    strategyOutput: Option[String],
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    roleCommands: Map[String, RuntimeCommand],
    interactorCommand: RuntimeCommand
  ): IO[Either[JudgeFailureReason, ProcessResult]] =
    val safeName = s"${subtask.index}-${testcase.index}"
    val inputPath = workingDirectory.resolve(s"interactive-$safeName.in")
    val outputPath = workingDirectory.resolve(s"interactive-$safeName.out")
    val strategyPath = workingDirectory.resolve(s"interactive-$safeName.strategy")
    val roleArgs = roleCommands.toList.sortBy(_._1).flatMap { case (role, command) => List(role, command.command) }
    val strategyArgs = strategyOutput match
      case Some(output) => List(strategyPath.getFileName.toString)
      case None => List("/dev/null")
    for
      _ <- IO.blocking {
        Files.write(inputPath, input)
        Files.deleteIfExists(outputPath)
        strategyOutput match
          case Some(output) => Files.writeString(strategyPath, output, StandardCharsets.UTF_8)
          case None => Files.deleteIfExists(strategyPath)
      }
      result <- sandbox.run(
        SandboxExecutionRequest(
          phase = s"interactor-$safeName",
          command = interactorCommand.command,
          args = interactorCommand.args ++ List(inputPath.getFileName.toString, outputPath.getFileName.toString) ++ strategyArgs ++ roleArgs,
          stdin = None,
          limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
          processLimit = math.max(2, interactorCommand.processLimit + roleCommands.size + 1)
        ),
        workingDirectory
      )
      output <- IO.blocking {
        if Files.exists(outputPath) then Files.readString(outputPath, StandardCharsets.UTF_8) else result.stdout
      }
    yield
      if result.timedOut || result.exitCode.getOrElse(-1) != 0 then Left(JudgeFailureReason.InteractorRuntimeFailed)
      else Right(result.copy(stdout = output))

  private def testcaseResult(
    testcase: JudgeTaskTestcase,
    score: BigDecimal,
    verdict: SubmissionVerdict,
    message: Option[String],
    reason: Option[JudgeFailureReason],
    runResult: ProcessResult
  ): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, clampScore(score), verdict, message, reason, runResult.timeUsedMs, runResult.memoryUsedKb)

  private def testcaseSystemError(testcase: JudgeTaskTestcase, reason: JudgeFailureReason): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), None, None)

  private def testcaseCompileError(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, BigDecimal(0), SubmissionVerdict.CompileError, None, None, None, None)

  private def testcaseAcceptedByProtocol(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, BigDecimal(1), SubmissionVerdict.AcceptedByProtocol, None, None, Some(0L), Some(0L))

  private def subtaskCompileError(subtask: JudgeTaskSubtask): JudgeSubtaskResult =
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      score = BigDecimal(0),
      verdict = SubmissionVerdict.CompileError,
      timeUsedMs = None,
      memoryUsedKb = None,
      reason = None,
      testcases = subtask.testcases.map(testcaseCompileError)
    )

  private def subtaskSystemError(subtask: JudgeTaskSubtask, reason: JudgeFailureReason): JudgeSubtaskResult =
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      score = BigDecimal(0),
      verdict = SubmissionVerdict.SystemError,
      timeUsedMs = None,
      memoryUsedKb = None,
      reason = Some(reason),
      testcases = subtask.testcases.map(testcase => testcaseSystemError(testcase, reason))
    )

  private def loadTestcaseData(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    problemDataCache: ProblemDataCache
  ): IO[Either[JudgeFailureReason, (Array[Byte], Option[Array[Byte]])]] =
    (for
      input <- problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, testcase.input)
      answerBytes <- testcase.answer.traverse(ref => problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, ref))
    yield (input, answerBytes)).attempt.map {
      case Right(data) => Right(data)
      case Left(_) => Left(JudgeFailureReason.ProblemDataLoadFailed)
    }

  private def scoreWithChecker(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    input: Array[Byte],
    contestantOutput: String,
    answerBytes: Option[Array[Byte]],
    compiledCheckers: Map[JudgeTaskFilePath, RuntimeCommand]
  ): IO[Either[JudgeFailureReason, CheckerScore]] =
    testcase.checker.`type` match
      case "builtin" if testcase.checker.name.contains("exact") =>
        answerBytes match
          case None => IO.pure(Left(JudgeFailureReason.CheckerRuntimeFailed))
          case Some(bytes) =>
            val expectedOutput = String(bytes, StandardCharsets.UTF_8)
            IO.pure(Right(CheckerScore(if normalizeOutput(contestantOutput) == normalizeOutput(expectedOutput) then BigDecimal(1) else BigDecimal(0), None)))
      case "builtin" if testcase.checker.name.contains("echo") =>
        IO.pure(parseCheckerStdout(contestantOutput))
      case "cpp17" | "cpp" =>
        testcase.checker.source match
          case Some(source) =>
            compiledCheckers.get(source.path) match
              case None => IO.pure(Left(JudgeFailureReason.CheckerCompileFailed))
              case Some(checkerCommand) =>
                val safeName = s"${testcase.index}-${math.abs(source.path.value.hashCode)}"
                val inputPath = workingDirectory.resolve(s"checker-$safeName.in")
                val outputPath = workingDirectory.resolve(s"checker-$safeName.out")
                val answerPath = workingDirectory.resolve(s"checker-$safeName.ans")
                for
                  _ <- IO.blocking {
                    Files.write(inputPath, input)
                    Files.writeString(outputPath, contestantOutput, StandardCharsets.UTF_8)
                    answerBytes match
                      case Some(bytes) => Files.write(answerPath, bytes)
                      case None => Files.deleteIfExists(answerPath)
                  }
                  checkerResult <- sandbox.run(
                    SandboxExecutionRequest(
                      phase = s"checker-${testcase.index}",
                      command = checkerCommand.command,
                      args = checkerCommand.args ++ List(
                        inputPath.getFileName.toString,
                        outputPath.getFileName.toString,
                        answerBytes.fold("/dev/null")(_ => answerPath.getFileName.toString)
                      ),
                      stdin = None,
                      limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value),
                      processLimit = checkerCommand.processLimit
                    ),
                    workingDirectory
                  )
                yield parseCheckerResult(checkerResult)
          case None => IO.pure(Left(JudgeFailureReason.SystemError))
      case _ => IO.pure(Left(JudgeFailureReason.SystemError))

  private def resolveCompilerPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.cxx) match
        case None => Left(s"Compiler '${config.cxx}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) => Right(path)
        case Some(path) => Left(s"Compiler '$path' is not visible inside isolate.")
    }

  private final case class CheckerScore(score: BigDecimal, message: Option[String])

  private def parseCheckerResult(result: ProcessResult): Either[JudgeFailureReason, CheckerScore] =
    if result.timedOut || result.exitCode.getOrElse(-1) != 0 then Left(JudgeFailureReason.CheckerRuntimeFailed)
    else parseCheckerStdout(result.stdout)

  private def parseCheckerStdout(stdout: String): Either[JudgeFailureReason, CheckerScore] =
    val trimmed = stdout.trim
    val firstWhitespace = trimmed.indexWhere(_.isWhitespace)
    val (rawScore, rawMessage) =
      if firstWhitespace < 0 then trimmed -> ""
      else trimmed.take(firstWhitespace) -> trimmed.drop(firstWhitespace).trim
    Try(BigDecimal(rawScore)).toEither
      .leftMap(_ => JudgeFailureReason.CheckerRuntimeFailed)
      .flatMap { score =>
        if score >= BigDecimal(0) && score <= BigDecimal(1) then Right(CheckerScore(score, Option.when(rawMessage.nonEmpty)(rawMessage)))
        else Left(JudgeFailureReason.CheckerRuntimeFailed)
      }

  private def aggregateSubtask(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): JudgeSubtaskResult =
    val score = aggregateScore(subtask.aggregation.score, testcases.map(_.score), subtask.testcases.map(_.scoreRatio))
    val verdict = aggregateVerdict(score, testcases.map(result => result.score -> result.verdict))
    val reason = Option.when(verdict == SubmissionVerdict.SystemError)(
      testcases.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )
    JudgeSubtaskResult(
      subtask.index,
      subtask.label,
      score,
      verdict,
      aggregateUsage(subtask.aggregation.time, testcases.flatMap(_.timeUsedMs)),
      aggregateUsage(subtask.aggregation.memory, testcases.flatMap(_.memoryUsedKb)),
      reason,
      testcases
    )

  private def aggregateTask(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): JudgeResult =
    val rawScore = aggregateScore(task.aggregation.score, subtasks.map(_.score), task.subtasks.map(_.scoreRatio))
    val score = roundFinalScore(rawScore, task.roundingScale)
    val verdict = aggregateVerdict(score, subtasks.map(result => result.score -> result.verdict))
    val reason = Option.when(verdict == SubmissionVerdict.SystemError)(
      subtasks.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )
    JudgeResult(score, verdict, reason, aggregateUsage(task.aggregation.time, subtasks.flatMap(_.timeUsedMs)), aggregateUsage(task.aggregation.memory, subtasks.flatMap(_.memoryUsedKb)), subtasks)

  private def aggregateScore(kind: String, scores: List[BigDecimal], ratios: List[BigDecimal]): BigDecimal =
    kind match
      case "min" => if scores.isEmpty then BigDecimal(0) else scores.min
      case "sum" => scores.zip(ratios).map { case (score, ratio) => score * ratio }.sum
      case _ => BigDecimal(0)

  private def aggregateUsage(kind: String, values: List[Long]): Option[Long] =
    if values.isEmpty then None
    else
      kind match
        case "sum" => Some(values.sum)
        case _ => Some(values.max)

  private def aggregateVerdict(score: BigDecimal, children: List[(BigDecimal, SubmissionVerdict)]): SubmissionVerdict =
    if score == BigDecimal(1) && children.exists(_._2 == SubmissionVerdict.AcceptedByProtocol) then SubmissionVerdict.AcceptedByProtocol
    else if score == BigDecimal(1) then SubmissionVerdict.Accepted
    else children.minByOption(_._1).map(_._2).getOrElse(SubmissionVerdict.SystemError)

  private def roundFinalScore(score: BigDecimal, scale: Int): BigDecimal =
    val roundedUp = score.setScale(scale, BigDecimal.RoundingMode.CEILING)
    if score < BigDecimal(1) && roundedUp >= BigDecimal(1) then BigDecimal(1) - BigDecimal(java.math.BigDecimal.ONE.movePointLeft(scale))
    else roundedUp

  private def clampScore(score: BigDecimal): BigDecimal =
    score.max(BigDecimal(0)).min(BigDecimal(1))

  private def containsSystemError(result: JudgeResult): Boolean =
    result.verdict == SubmissionVerdict.SystemError ||
      result.subtasks.exists(subtask => subtask.verdict == SubmissionVerdict.SystemError || subtask.testcases.exists(_.verdict == SubmissionVerdict.SystemError))

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private def uniqueRefs(refs: List[JudgeTaskFileRef]): List[JudgeTaskFileRef] =
    refs.groupBy(_.path).values.map(_.head).toList

  private val MinimalTestlibHeader: String =
    """#pragma once
      |#include <bits/stdc++.h>
      |using namespace std;
      |
      |enum TResult { _ok, _wa, _pe, _fail };
      |
      |class InStream {
      |  ifstream file;
      |public:
      |  InStream() = default;
      |  explicit InStream(const char* path) { file.open(path); }
      |  void init(const char* path) { file.open(path); }
      |  int readInt() { int value; file >> value; return value; }
      |  long long readLong() { long long value; file >> value; return value; }
      |  double readDouble() { double value; file >> value; return value; }
      |  string readString() { string value; file >> value; return value; }
      |  string readLine() { string value; getline(file, value); return value; }
      |  bool eof() { return file.eof(); }
      |  bool seekEof() { file >> ws; return file.eof(); }
      |};
      |
      |static InStream inf;
      |static InStream ouf;
      |static InStream ans;
      |
      |inline void registerTestlibCmd(int argc, char* argv[]) {
      |  if (argc < 4) {
      |    cerr << "checker requires input, output, and answer paths";
      |    exit(3);
      |  }
      |  inf.init(argv[1]);
      |  ouf.init(argv[2]);
      |  ans.init(argv[3]);
      |}
      |
      |inline void quitf(TResult result, const char* format, ...) {
      |  if (result == _ok) {
      |    cout << "1";
      |    exit(0);
      |  }
      |  if (result == _wa || result == _pe) {
      |    cout << "0";
      |    exit(0);
      |  }
      |  exit(3);
      |}
      |
      |inline void quitp(double score, const char* format = "", ...) {
      |  cout << max(0.0, min(1.0, score));
      |  exit(0);
      |}
      |""".stripMargin
