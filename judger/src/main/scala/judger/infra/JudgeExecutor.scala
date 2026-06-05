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
import java.nio.file.attribute.PosixFilePermissions
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

  private final case class StrategyProviderRuntime(
    tool: JudgeTaskTool,
    command: RuntimeCommand
  )

  private final case class InteractiveRunResult(
    interactor: ProcessResult,
    participants: Map[String, ProcessResult],
    strategyProvider: Option[ProcessResult],
    output: String,
    status: Option[String]
  )

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
              case Right(tools) => judgeSubtasks(task, config, workingDirectory, sandbox, problemDataCache, programs, tools)
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
    val interactorSources = uniqueRefs(task.subtasks.flatMap(_.mode.interactor.map(_.source)))
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
    config: AppConfig,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    programs: PreparedPrograms,
    tools: PreparedTools
  ): IO[ReportJudgeResultRequest] =
    task.subtasks.traverse(subtask => judgeSubtask(task, config, subtask, workingDirectory, sandbox, problemDataCache, programs, tools)).map { subtasks =>
      val result = aggregateTask(task, subtasks)
      ReportJudgeResultRequest(
        status = if containsSystemError(result) then SubmissionStatus.Failed else SubmissionStatus.Completed,
        judgeResult = Some(result)
      )
    }

  private def judgeSubtask(
    task: JudgeTask,
    config: AppConfig,
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
            subtask.mode.interactor.flatMap(interactor => tools.interactors.get(interactor.source.path).map(interactor -> _)) match
              case None => IO.pure(subtaskSystemError(subtask, JudgeFailureReason.InteractorCompileFailed))
              case Some((interactor, interactorCommand)) =>
                val roleCommands = subtask.mode.roles.flatMap(role => programCommand(task, programs, role).map(role -> _)).toMap
                subtask.testcases.traverse { testcase =>
                  judgeInteractiveTestcase(task, config, subtask, testcase, workingDirectory, sandbox, problemDataCache, roleCommands, interactor, interactorCommand, tools)
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
    config: AppConfig,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    roleCommands: Map[String, RuntimeCommand],
    interactor: JudgeTaskTool,
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
            strategyProviderRuntime(testcase, tools) match
              case Left(_) =>
                IO.pure(testcaseAcceptedByProtocol(testcase))
              case Right(strategyProvider) =>
                runInteractiveProcesses(config, subtask, testcase, input, workingDirectory, sandbox, roleCommands, interactor, interactorCommand, strategyProvider)
                  .flatMap {
                    case Left(reason) =>
                      IO.pure(testcaseSystemError(testcase, reason))
                    case Right(runResult) if interactiveToolAcceptedByProtocol(interactor, strategyProvider, runResult) =>
                      IO.pure(testcaseAcceptedByProtocol(testcase))
                    case Right(runResult) if runResult.status.contains("accepted_by_protocol") =>
                      IO.pure(testcaseAcceptedByProtocol(testcase))
                    case Right(runResult) if strategyFailed(strategyProvider, runResult.strategyProvider) =>
                      IO.pure(testcaseAcceptedByProtocol(testcase))
                    case Right(runResult) if runResult.interactor.exitCode.getOrElse(-1) != 0 =>
                      IO.pure(testcaseSystemError(testcase, JudgeFailureReason.InteractorRuntimeFailed))
                    case Right(runResult) =>
                      participantFailure(runResult.participants) match
                        case Some((verdict, participantResult)) =>
                          IO.pure(testcaseResult(testcase, BigDecimal(0), verdict, None, None, participantResult))
                        case None =>
                          scoreWithChecker(task, testcase, workingDirectory, sandbox, input, runResult.output, answerBytes, tools.checkers).map {
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

  private def strategyProviderRuntime(
    testcase: JudgeTaskTestcase,
    tools: PreparedTools
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

  private def runInteractiveProcesses(
    config: AppConfig,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    roleCommands: Map[String, RuntimeCommand],
    interactor: JudgeTaskTool,
    interactorCommand: RuntimeCommand,
    strategyProvider: Option[StrategyProviderRuntime]
  ): IO[Either[JudgeFailureReason, InteractiveRunResult]] =
    interactor.limits match
      case None => IO.pure(Left(JudgeFailureReason.SystemError))
      case Some(interactorLimits) =>
        val safeName = s"${subtask.index}-${testcase.index}"
        val interactiveDir = workingDirectory.resolve(s"interactive-$safeName")
        val inputPath = interactiveDir.resolve("input")
        val outputPath = interactiveDir.resolve("output")
        val statusPath = interactiveDir.resolve("status")
        val roleFifos =
          subtask.mode.roles.flatMap { role =>
            roleCommands.get(role).map { command =>
              val safeRole = sanitizeInteractiveName(role)
              (role, command, interactiveDir.resolve(s"to-participant-$safeRole"), interactiveDir.resolve(s"from-participant-$safeRole"))
            }
          }
        val strategyFifos = strategyProvider.map(_ => interactiveDir.resolve("to-strategy") -> interactiveDir.resolve("from-strategy"))
        val fifoPaths = roleFifos.flatMap { case (_, _, toParticipant, fromParticipant) => List(toParticipant, fromParticipant) } ++
          strategyFifos.toList.flatMap { case (toStrategy, fromStrategy) => List(toStrategy, fromStrategy) }
        val participantLimits =
          SandboxLimits.runtimeWithWall(
            timeLimitMs = testcase.limits.timeMs.value.toLong,
            wallTimeLimitMs = interactiveParticipantWallTimeMs(testcase, interactor, strategyProvider),
            memoryLimitMb = testcase.limits.memoryMb.value
          )
        val interactorArgs =
          interactorCommand.args ++
            List(relativeSandboxPath(workingDirectory, inputPath), relativeSandboxPath(workingDirectory, outputPath), relativeSandboxPath(workingDirectory, statusPath)) ++
            roleFifos.flatMap { case (role, _, toParticipant, fromParticipant) =>
              List(role, relativeSandboxPath(workingDirectory, toParticipant), relativeSandboxPath(workingDirectory, fromParticipant))
            } ++
            strategyFifos.toList.flatMap { case (toStrategy, fromStrategy) =>
              List("strategy", relativeSandboxPath(workingDirectory, toStrategy), relativeSandboxPath(workingDirectory, fromStrategy))
            }

        val run =
          for
            sigpipeLauncher <- ensureSigpipeIgnoreLauncher(config, workingDirectory)
            fifoRedirectLauncher <- ensureFifoRedirectLauncher(config, workingDirectory)
            _ <- prepareInteractiveWorkspace(interactiveDir, inputPath, outputPath, statusPath, fifoPaths, input)
            participantFibers <- roleFifos.zipWithIndex.traverse { case ((role, command, toParticipant, fromParticipant), index) =>
              sandbox
                .runInBox(
                  index + 1,
                  SandboxExecutionRequest(
                    phase = s"participant-$safeName-$role",
                    command = fifoRedirectLauncher.command,
                    args = fifoRedirectLauncher.args ++ List(
                      relativeSandboxPath(workingDirectory, toParticipant),
                      relativeSandboxPath(workingDirectory, fromParticipant),
                      command.command
                    ) ++ command.args,
                    stdin = None,
                    limits = participantLimits,
                    processLimit = command.processLimit,
                    captureStdout = false
                  ),
                  workingDirectory
                )
                .map(role -> _)
                .start
            }
            strategyFiber <- strategyProvider.traverse { provider =>
              val (toStrategy, fromStrategy) = strategyFifos.get
              val limits = provider.tool.limits.get
              sandbox
                .runInBox(
                  roleFifos.size + 1,
                  SandboxExecutionRequest(
                    phase = s"strategy-$safeName",
                    command = fifoRedirectLauncher.command,
                    args = fifoRedirectLauncher.args ++ List(
                      relativeSandboxPath(workingDirectory, toStrategy),
                      relativeSandboxPath(workingDirectory, fromStrategy),
                      provider.command.command
                    ) ++ provider.command.args,
                    stdin = None,
                    limits = SandboxLimits.realTime(limits.realTimeMs.value.toLong, limits.memoryMb.value),
                    processLimit = provider.command.processLimit,
                    captureStdout = false
                  ),
                  workingDirectory
                )
                .start
            }
            interactorResult <- sandbox.runInBox(
              roleFifos.size + strategyProvider.fold(0)(_ => 1) + 1,
              SandboxExecutionRequest(
                phase = s"interactor-$safeName",
                command = sigpipeLauncher.command,
                args = sigpipeLauncher.args ++ List(interactorCommand.command) ++ interactorArgs,
                stdin = None,
                limits = SandboxLimits.realTime(interactorLimits.realTimeMs.value.toLong, interactorLimits.memoryMb.value),
                processLimit = interactorCommand.processLimit
              ),
              workingDirectory
            )
            participantResults <- participantFibers.traverse(joinFiber)
            strategyResult <- strategyFiber.traverse(joinFiber)
            outputAndStatus <- IO.blocking {
              val output =
                if Files.exists(outputPath) && Files.isRegularFile(outputPath) then Files.readString(outputPath, StandardCharsets.UTF_8)
                else interactorResult.stdout
              val status =
                if Files.exists(statusPath) && Files.isRegularFile(statusPath) then
                  Option(Files.readString(statusPath, StandardCharsets.UTF_8).trim).filter(_.nonEmpty)
                else None
              output -> status
            }
          yield
            val (output, status) = outputAndStatus
            Right(
              InteractiveRunResult(
                interactor = interactorResult,
                participants = participantResults.toMap,
                strategyProvider = strategyResult,
                output = output,
                status = status
              )
            )

        run.handleError(_ => Left(JudgeFailureReason.JudgerRuntimeFailed))

  private def prepareInteractiveWorkspace(
    interactiveDir: Path,
    inputPath: Path,
    outputPath: Path,
    statusPath: Path,
    fifoPaths: List[Path],
    input: Array[Byte]
  ): IO[Unit] =
    IO.blocking {
      Files.createDirectories(interactiveDir)
      setWorldAccessible(interactiveDir)
      Files.write(inputPath, input)
      setWorldReadable(inputPath)
      Files.deleteIfExists(outputPath)
      Files.deleteIfExists(statusPath)
      fifoPaths.foreach(createFifo)
    }

  private def createFifo(path: Path): Unit =
    Files.deleteIfExists(path)
    val process = new ProcessBuilder("mkfifo", path.toString).start()
    val exitCode = process.waitFor()
    if exitCode != 0 then
      val detail = nonEmptyOrFallback(IsolateSandbox.readStream(process.getErrorStream), IsolateSandbox.readStream(process.getInputStream), "mkfifo failed")
      throw RuntimeException(detail)
    setWorldFifo(path)

  private def setWorldAccessible(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx")))
    ()

  private def setWorldReadable(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-rw-")))
    ()

  private def setWorldFifo(path: Path): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-rw-")))
    ()

  private def ensureSigpipeIgnoreLauncher(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "sigpipe-ignore.cpp",
      executableName = "sigpipe-ignore",
      source = SigpipeIgnoreLauncherSource
    )

  private def ensureFifoRedirectLauncher(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "fifo-redirect.cpp",
      executableName = "fifo-redirect",
      source = FifoRedirectLauncherSource
    )

  private def ensureCompiledLauncher(
    config: AppConfig,
    workingDirectory: Path,
    sourceName: String,
    executableName: String,
    source: String
  ): IO[RuntimeCommand] =
    val executablePath = workingDirectory.resolve(executableName)
    if Files.exists(executablePath) && Files.isExecutable(executablePath) then
      IO.pure(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1))
    else
      resolveCompilerPath(config).flatMap {
        case Left(message) => IO.raiseError(RuntimeException(message))
        case Right(compilerPath) =>
          for
            _ <- IO.blocking(Files.writeString(workingDirectory.resolve(sourceName), source, StandardCharsets.UTF_8))
            compileResult <- runHostProcess(
              command = compilerPath,
              args = List(sourceName, "-o", executableName, "-O2"),
              cwd = workingDirectory,
              stdin = None,
              limits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048),
              stdoutName = s".$executableName.compile.stdout",
              stderrName = s".$executableName.compile.stderr"
            )
            _ <-
              if compileResult.timedOut || compileResult.exitCode.getOrElse(-1) != 0 then
                IO.raiseError(RuntimeException(s"Failed to compile $executableName launcher."))
              else ensureExecutableExists(executablePath)
          yield RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1)
      }

  private def interactiveParticipantWallTimeMs(
    testcase: JudgeTaskTestcase,
    interactor: JudgeTaskTool,
    strategyProvider: Option[StrategyProviderRuntime]
  ): Long =
    testcase.limits.timeMs.value.toLong +
      interactor.limits.map(_.realTimeMs.value.toLong).getOrElse(0L) +
      strategyProvider.flatMap(_.tool.limits).map(_.realTimeMs.value.toLong).getOrElse(0L) +
      5000L

  private def interactiveToolAcceptedByProtocol(
    interactor: JudgeTaskTool,
    strategyProvider: Option[StrategyProviderRuntime],
    runResult: InteractiveRunResult
  ): Boolean =
    toolRealTimeExceeded(interactor, runResult.interactor) ||
      strategyProvider.exists(provider => runResult.strategyProvider.exists(result => toolRealTimeExceeded(provider.tool, result)))

  private def toolRealTimeExceeded(tool: JudgeTaskTool, result: ProcessResult): Boolean =
    tool.limits.exists(limits =>
      result.timedOut || result.wallTimeUsedMs.exists(_ > limits.realTimeMs.value.toLong)
    )

  private def strategyFailed(strategyProvider: Option[StrategyProviderRuntime], result: Option[ProcessResult]): Boolean =
    strategyProvider.exists(_ =>
      result match
        case None => true
        case Some(current) => current.exitCode.getOrElse(-1) != 0
    )

  private def participantFailure(participants: Map[String, ProcessResult]): Option[(SubmissionVerdict, ProcessResult)] =
    participants.toList.sortBy(_._1).collectFirst {
      case (_, result) if result.timedOut => SubmissionVerdict.TimeLimitExceeded -> result
      case (_, result) if result.exitCode.getOrElse(-1) != 0 => SubmissionVerdict.RuntimeError -> result
    }

  private def relativeSandboxPath(workingDirectory: Path, path: Path): String =
    workingDirectory.relativize(path).toString

  private def sanitizeInteractiveName(value: String): String =
    value.map {
      case current if current.isLetterOrDigit || current == '-' || current == '_' => current
      case _ => '_'
    }

  private def joinFiber[A](fiber: cats.effect.FiberIO[A]): IO[A] =
    fiber.join.flatMap {
      case cats.effect.Outcome.Succeeded(result) => result
      case cats.effect.Outcome.Errored(error) => IO.raiseError(error)
      case cats.effect.Outcome.Canceled() => IO.canceled *> IO.never[A]
    }

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

  private val SigpipeIgnoreLauncherSource: String =
    """#include <signal.h>
      |#include <stdio.h>
      |#include <unistd.h>
      |
      |int main(int argc, char** argv) {
      |  if (argc < 2) {
      |    return 127;
      |  }
      |  if (signal(SIGPIPE, SIG_IGN) == SIG_ERR) {
      |    return 127;
      |  }
      |  execvp(argv[1], argv + 1);
      |  perror("execvp");
      |  return 127;
      |}
      |""".stripMargin

  private val FifoRedirectLauncherSource: String =
    """#include <errno.h>
      |#include <fcntl.h>
      |#include <stdio.h>
      |#include <unistd.h>
      |
      |static int open_stdout_fifo(const char* path) {
      |  for (;;) {
      |    int fd = open(path, O_WRONLY | O_NONBLOCK);
      |    if (fd >= 0) {
      |      return fd;
      |    }
      |    if (errno != ENXIO && errno != EINTR) {
      |      perror("open stdout fifo");
      |      return -1;
      |    }
      |    usleep(1000);
      |  }
      |}
      |
      |static void clear_nonblock(int fd) {
      |  int flags = fcntl(fd, F_GETFL, 0);
      |  if (flags >= 0) {
      |    fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
      |  }
      |}
      |
      |int main(int argc, char** argv) {
      |  if (argc < 4) {
      |    return 127;
      |  }
      |
      |  int stdin_fd = open(argv[1], O_RDONLY | O_NONBLOCK);
      |  if (stdin_fd < 0) {
      |    perror("open stdin fifo");
      |    return 127;
      |  }
      |
      |  int stdout_fd = open_stdout_fifo(argv[2]);
      |  if (stdout_fd < 0) {
      |    close(stdin_fd);
      |    return 127;
      |  }
      |
      |  clear_nonblock(stdin_fd);
      |  clear_nonblock(stdout_fd);
      |
      |  if (dup2(stdin_fd, STDIN_FILENO) < 0) {
      |    perror("dup2 stdin");
      |    return 127;
      |  }
      |  if (dup2(stdout_fd, STDOUT_FILENO) < 0) {
      |    perror("dup2 stdout");
      |    return 127;
      |  }
      |
      |  if (stdin_fd != STDIN_FILENO) {
      |    close(stdin_fd);
      |  }
      |  if (stdout_fd != STDOUT_FILENO) {
      |    close(stdout_fd);
      |  }
      |
      |  execvp(argv[3], argv + 3);
      |  perror("execvp");
      |  return 127;
      |}
      |""".stripMargin
