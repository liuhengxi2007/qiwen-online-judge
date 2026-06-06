package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.{ReportHackResultRequest, ReportJudgeResultRequest}
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
    validators: Map[JudgeTaskFilePath, RuntimeCommand],
    checkers: Map[JudgeTaskFilePath, RuntimeCommand],
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
    status: Option[String],
    strategyProviderReadMonitor: Option[StrategyProviderReadMonitor]
  )

  private final case class StrategyProviderReadMonitor(
    librarySandboxPath: String,
    targetFifoSandboxPath: String,
    logPath: Path,
    logSandboxPath: String,
    idleLimitMs: Long
  )

  private final case class ScoreAggregation(
    score: BigDecimal,
    lowestScore: BigDecimal,
    verdictChildren: List[(BigDecimal, SubmissionVerdict)]
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

  def hack(
    task: HackTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportHackResultRequest] =
    withWorkingDirectory(config.workRoot, "qiwen-hack-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        preparePrograms(task.targetTask, config, workingDirectory, runtimes).flatMap {
          case Left(result) => IO.pure(hackFailed(task, result.judgeResult.flatMap(_.reason).map(JudgeFailureReason.render).getOrElse("Target prepare failed.")))
          case Right(programs) =>
            prepareTools(task.targetTask, config, workingDirectory, problemDataCache).flatMap {
              case Left(result) => IO.pure(hackFailed(task, result.judgeResult.flatMap(_.reason).map(JudgeFailureReason.render).getOrElse("Tool prepare failed.")))
              case Right(tools) => executeHack(task, config, workingDirectory, sandbox, problemDataCache, runtimes, programs, tools)
            }
        }
      }
    }.handleError { error =>
      hackFailed(task, Option(error.getMessage).getOrElse("Hack execution failed."))
    }

  private def executeHack(
    hackTask: HackTask,
    config: AppConfig,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime],
    programs: PreparedPrograms,
    tools: PreparedTools
  ): IO[ReportHackResultRequest] =
    val task = hackTask.targetTask
    task.subtasks.find(_.index == hackTask.subtaskIndex) match
      case None =>
        IO.pure(hackFailed(hackTask, "Target subtask was not found."))
      case Some(subtask) =>
        val input = hackTask.input.getBytes(StandardCharsets.UTF_8)
        val oldLowestScore = targetSubtaskLowestScore(hackTask)
        subtask.validator match
          case None => IO.pure(hackFailed(hackTask, "Target subtask has no validator."))
          case Some(validator) =>
            runValidator(hackTask, subtask, input, workingDirectory, sandbox, validator, tools).flatMap {
              case Some(message) =>
                IO.pure(
                  ReportHackResultRequest(
                    status = "invalid",
                    answer = None,
                    oldScore = oldLowestScore,
                    newScore = None,
                    newResult = None,
                    validatorMessage = Some(message),
                    standardMessage = None,
                    targetMessage = None
                  )
                )
              case None =>
                runStandard(hackTask, subtask, input, config, workingDirectory, sandbox, problemDataCache, runtimes).flatMap {
                  case Left(message) => IO.pure(hackFailed(hackTask, message, standardMessage = Some(message)))
                  case Right(answer) =>
                    runTargetOnHack(hackTask, subtask, input, answer, config, workingDirectory, sandbox, problemDataCache, programs, tools).map { testcaseResult =>
                      val oldSubtask = hackTask.oldResult.subtasks.find(_.index == subtask.index)
                      val existingTestcases = oldSubtask.map(_.testcases).getOrElse(Nil)
                      val newSubtask = aggregateSubtask(subtask, existingTestcases :+ testcaseResult)
                      val newSubtasks =
                        if hackTask.oldResult.subtasks.exists(_.index == subtask.index) then
                          hackTask.oldResult.subtasks.map(current => if current.index == subtask.index then newSubtask else current)
                        else hackTask.oldResult.subtasks :+ newSubtask
                      val newResult = aggregateTask(task, newSubtasks)
                      val targetMessage = Some(s"${SubmissionVerdict.render(testcaseResult.verdict)} score=${testcaseResult.score}")
                      val status = if newSubtask.lowestScore < oldLowestScore then "success" else "no_effect"
                      ReportHackResultRequest(
                        status = status,
                        answer = Some(new String(answer, StandardCharsets.UTF_8)),
                        oldScore = oldLowestScore,
                        newScore = Some(newSubtask.lowestScore),
                        newResult = Some(newResult),
                        validatorMessage = Some("accepted"),
                        standardMessage = Some("accepted"),
                        targetMessage = targetMessage
                      )
                    }
                }
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

  private def runValidator(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    validator: JudgeTaskTool,
    tools: PreparedTools
  ): IO[Option[String]] =
    tools.validators.get(validator.source.path) match
      case None => IO.pure(Some("Validator could not be prepared."))
      case Some(command) =>
        val limits = validator.limits
          .map(limits => SandboxLimits.runtime(limits.timeMs.value.toLong, limits.memoryMb.value))
          .getOrElse(SandboxLimits.runtime(timeLimitMs = 5000L, memoryLimitMb = 512))
        sandbox
          .run(
            SandboxExecutionRequest(
              phase = s"hack-validator-${hackTask.hackId}-${subtask.index}",
              command = command.command,
              args = command.args,
              stdin = Some(input),
              limits = limits,
              processLimit = command.processLimit
            ),
            workingDirectory
          )
          .map { result =>
            if !result.timedOut && result.exitCode.contains(0) then None
            else Some(renderDetail("Validator rejected the hack input.", result))
          }

  private def runStandard(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    config: AppConfig,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[Either[String, Array[Byte]]] =
    (subtask.standard, templateHackTestcase(subtask)) match
      case (None, _) => IO.pure(Left("Target subtask has no standard solution."))
      case (_, None) => IO.pure(Left("Target subtask has no testcase template."))
      case (Some(standard), Some(template)) =>
        runtimes.get(standard.language) match
          case None => IO.pure(Left("Standard language is not supported by this judger."))
          case Some(runtime) =>
            problemDataCache.loadBytes(hackTask.targetTask.problemSlug, hackTask.targetTask.problemDataVersion, standard.source).attempt.flatMap {
              case Left(error) => IO.pure(Left(s"Standard source could not be loaded: ${Option(error.getMessage).getOrElse(error.getClass.getName)}"))
              case Right(sourceBytes) =>
                val sourceCode = SubmissionSourceCode(new String(sourceBytes, StandardCharsets.UTF_8))
                runtime.prepare(s"standard-${hackTask.hackId}-${subtask.index}", sourceCode, config, workingDirectory).flatMap {
                  case Left(ProgramPrepareFailure.CompileError) => IO.pure(Left("Standard solution failed to compile."))
                  case Left(ProgramPrepareFailure.SystemError(reason)) => IO.pure(Left(s"Standard solution failed to prepare: ${JudgeFailureReason.render(reason)}."))
                  case Right(command) =>
                    sandbox
                      .run(
                        SandboxExecutionRequest(
                          phase = s"hack-standard-${hackTask.hackId}-${subtask.index}",
                          command = command.command,
                          args = command.args,
                          stdin = Some(input),
                          limits = SandboxLimits.runtime(template.limits.timeMs.value.toLong, template.limits.memoryMb.value),
                          processLimit = command.processLimit
                        ),
                        workingDirectory
                      )
                      .map { result =>
                        if result.timedOut then Left(renderDetail("Standard solution timed out.", result))
                        else if result.exitCode.getOrElse(-1) != 0 then Left(renderDetail("Standard solution failed at runtime.", result))
                        else Right(result.stdout.getBytes(StandardCharsets.UTF_8))
                      }
                }
            }

  private def runTargetOnHack(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    answer: Array[Byte],
    config: AppConfig,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    programs: PreparedPrograms,
    tools: PreparedTools
  ): IO[JudgeTestcaseResult] =
    templateHackTestcase(subtask) match
      case None => IO.pure(JudgeTestcaseResult(1, Some("hack"), JudgeTestcaseType.Hack, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(JudgeFailureReason.SystemError), None, None))
      case Some(testcase) =>
        subtask.mode.`type` match
          case "traditional" =>
            val role = subtask.mode.role.getOrElse("main")
            programCommand(hackTask.targetTask, programs, role) match
              case None => IO.pure(testcaseCompileError(testcase))
              case Some(command) =>
                runTraditionalTestcaseData(hackTask.targetTask, subtask, testcase, workingDirectory, sandbox, input, Some(answer), command, tools)
          case "interactive" =>
            runInteractiveHackTestcase(hackTask, config, subtask, testcase, input, answer, workingDirectory, sandbox, programs, tools)
          case _ =>
            IO.pure(testcaseSystemError(testcase, JudgeFailureReason.SystemError))

  private def runInteractiveHackTestcase(
    hackTask: HackTask,
    config: AppConfig,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    answer: Array[Byte],
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    programs: PreparedPrograms,
    tools: PreparedTools
  ): IO[JudgeTestcaseResult] =
    val missingRole = subtask.mode.roles.find(role => programCommand(hackTask.targetTask, programs, role).isEmpty)
    missingRole match
      case Some(_) => IO.pure(testcaseCompileError(testcase))
      case None =>
        subtask.mode.interactor.flatMap(interactor => tools.interactors.get(interactor.source.path).map(interactor -> _)) match
          case None => IO.pure(testcaseSystemError(testcase, JudgeFailureReason.InteractorCompileFailed))
          case Some((interactor, interactorCommand)) =>
            prepareHackStrategyProvider(hackTask, testcase, config, workingDirectory).flatMap {
              case Left(_) => IO.pure(testcaseAcceptedByProtocol(testcase))
              case Right(strategyProvider) =>
                val roleCommands = subtask.mode.roles.flatMap(role => programCommand(hackTask.targetTask, programs, role).map(role -> _)).toMap
                runInteractiveProcesses(config, subtask, testcase, input, workingDirectory, sandbox, roleCommands, interactor, interactorCommand, strategyProvider)
                  .flatMap {
                    case Left(reason) =>
                      IO.pure(testcaseSystemError(testcase, reason))
                    case Right(runResult) if interactiveToolCpuLimitExceeded(interactor, strategyProvider.map(_.tool), runResult.interactor, runResult.strategyProvider) =>
                      IO.pure(testcaseAcceptedByProtocol(testcase))
                    case Right(runResult) =>
                      val timeoutProcesses = interactiveProcessesForTimeout(testcase, interactor, strategyProvider, runResult)
                      if interactiveWallOnlyTimeout(timeoutProcesses) then
                        readStrategyProviderWaitMs(runResult.strategyProviderReadMonitor, runResult.interactor).map { readWaitMs =>
                          interactiveWallOnlyVerdict(
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
                        scoreInteractiveRun(hackTask.targetTask, testcase, workingDirectory, sandbox, input, Some(answer), tools, strategyProvider, runResult)
                  }
            }

  private def prepareHackStrategyProvider(
    hackTask: HackTask,
    testcase: JudgeTaskTestcase,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[Unit, Option[StrategyProviderRuntime]]] =
    testcase.strategyProvider match
      case None => IO.pure(Right(None))
      case Some(provider) if provider.limits.isEmpty => IO.pure(Left(()))
      case Some(provider) =>
        hackTask.strategyProviderSource match
          case None => IO.pure(Left(()))
          case Some(source) =>
            compileCppToolBytes(
              task = hackTask.targetTask,
              config = config,
              workingDirectory = workingDirectory,
              sourceNameHint = s"hack-strategy-${hackTask.hackId}",
              sourceBytes = source.getBytes(StandardCharsets.UTF_8)
            ).map {
              case ToolCompileOutcome.Success(command) => Right(Some(StrategyProviderRuntime(provider, command)))
              case _ => Left(())
            }

  private def templateHackTestcase(subtask: JudgeTaskSubtask): Option[JudgeTaskTestcase] =
    subtask.testcases.find(_.testcaseType != JudgeTestcaseType.Hack).orElse(subtask.testcases.headOption).map { template =>
      val nextIndex = subtask.testcases.map(_.index).maxOption.getOrElse(0) + 1
      template.copy(index = nextIndex, label = Some("hack"), testcaseType = JudgeTestcaseType.Hack, scoreRatio = BigDecimal(1), answer = None)
    }

  private def hackFailed(
    task: HackTask,
    targetMessage: String,
    standardMessage: Option[String] = None
  ): ReportHackResultRequest =
    ReportHackResultRequest(
      status = "failed",
      answer = None,
      oldScore = targetSubtaskLowestScore(task),
      newScore = None,
      newResult = None,
      validatorMessage = None,
      standardMessage = standardMessage,
      targetMessage = Some(targetMessage)
    )

  private def targetSubtaskLowestScore(task: HackTask): BigDecimal =
    task.oldResult.subtasks.find(_.index == task.subtaskIndex).map(_.lowestScore).getOrElse(BigDecimal(0))

  private def prepareTools(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    problemDataCache: ProblemDataCache
  ): IO[Either[ReportJudgeResultRequest, PreparedTools]] =
    val validatorSources = uniqueRefs(task.subtasks.flatMap(_.validator.map(_.source)))
    val checkerSources = uniqueRefs(task.subtasks.flatMap(_.testcases).flatMap(_.checker.source))
    val interactorSources = uniqueRefs(task.subtasks.flatMap(_.mode.interactor.map(_.source)))
    val strategyProviderSources = uniqueRefs(task.subtasks.flatMap(_.testcases).flatMap(_.strategyProvider.map(_.source)))

    for
      validators <- compileRequiredTools(task, config, workingDirectory, problemDataCache, validatorSources, JudgeFailureReason.CheckerCompileFailed)
      checkers <- validators match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileRequiredTools(task, config, workingDirectory, problemDataCache, checkerSources, JudgeFailureReason.CheckerCompileFailed)
      interactors <- checkers match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileRequiredTools(task, config, workingDirectory, problemDataCache, interactorSources, JudgeFailureReason.InteractorCompileFailed)
      strategyProviders <- interactors match
        case Left(result) => IO.pure(Left(result))
        case Right(_) => compileStrategyProviders(task, config, workingDirectory, problemDataCache, strategyProviderSources)
    yield
      (validators, checkers, interactors, strategyProviders) match
        case (Right(validatorCommands), Right(checkerCommands), Right(interactorCommands), Right((strategyCommands, failedStrategies))) =>
          Right(PreparedTools(validatorCommands, checkerCommands, interactorCommands, strategyCommands, failedStrategies))
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
    problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, sourceRef).attempt.flatMap {
      case Left(_) =>
        IO.pure(ToolCompileOutcome.SystemFailed(JudgeFailureReason.ProblemDataLoadFailed))
      case Right(sourceBytes) =>
        compileCppToolBytes(task, config, workingDirectory, sourceRef.path.value, sourceBytes)
    }

  private def compileCppToolBytes(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    sourceNameHint: String,
    sourceBytes: Array[Byte]
  ): IO[ToolCompileOutcome] =
    val _ = task
    resolveCompilerPath(config).flatMap {
      case Left(_) => IO.pure(ToolCompileOutcome.CompileFailed)
      case Right(compilerPath) =>
        val safeHash = math.abs(sourceNameHint.hashCode)
        val sourceName = s"tool-$safeHash.cpp"
        val executableName = s"tool-$safeHash"
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
        runTraditionalTestcaseData(task, subtask, testcase, workingDirectory, sandbox, input, answerBytes, command, tools)
    }

  private def runTraditionalTestcaseData(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    command: RuntimeCommand,
    tools: PreparedTools
  ): IO[JudgeTestcaseResult] =
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
        strategyProviderRuntime(testcase, tools) match
          case Left(_) =>
            IO.pure(testcaseAcceptedByProtocol(testcase))
          case Right(strategyProvider) =>
            runInteractiveProcesses(config, subtask, testcase, input, workingDirectory, sandbox, roleCommands, interactor, interactorCommand, strategyProvider)
              .flatMap {
                case Left(reason) =>
                  IO.pure(testcaseSystemError(testcase, reason))
                case Right(runResult) if interactiveToolCpuLimitExceeded(interactor, strategyProvider.map(_.tool), runResult.interactor, runResult.strategyProvider) =>
                  IO.pure(testcaseAcceptedByProtocol(testcase))
                case Right(runResult) =>
                  val timeoutProcesses = interactiveProcessesForTimeout(testcase, interactor, strategyProvider, runResult)
                  if interactiveWallOnlyTimeout(timeoutProcesses) then
                    readStrategyProviderWaitMs(runResult.strategyProviderReadMonitor, runResult.interactor).map { readWaitMs =>
                      interactiveWallOnlyVerdict(
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
    }

  private def scoreInteractiveRun(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    tools: PreparedTools,
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
      participantFailure(runResult.participants, testcase.limits.timeMs.value.toLong) match
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
        val strategyReadMonitorPaths =
          strategyProvider.flatMap { provider =>
            strategyFifos.map { case (_, fromStrategy) =>
              (
                fromStrategy,
                interactiveDir.resolve("strategy-provider-read-monitor.log"),
                provider.tool.limits.map(_.timeMs.value.toLong).getOrElse(0L)
              )
            }
          }
        val fifoPaths = roleFifos.flatMap { case (_, _, toParticipant, fromParticipant) => List(toParticipant, fromParticipant) } ++
          strategyFifos.toList.flatMap { case (toStrategy, fromStrategy) => List(toStrategy, fromStrategy) }
        val sharedWallTimeLimitMs =
          interactiveWallTimeLimitMs(
            testcase = testcase,
            roleCount = subtask.mode.roles.size,
            interactor = interactor,
            strategyProvider = strategyProvider.map(_.tool)
          )
        val participantLimits =
          SandboxLimits.runtimeWithWall(
            timeLimitMs = testcase.limits.timeMs.value.toLong,
            wallTimeLimitMs = sharedWallTimeLimitMs,
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
            readMonitorLibrary <- strategyReadMonitorPaths.traverse(_ => ensureStrategyProviderReadMonitor(config, workingDirectory))
            strategyReadMonitor =
              strategyReadMonitorPaths.zip(readMonitorLibrary).map { case ((fromStrategy, logPath, idleLimitMs), library) =>
                StrategyProviderReadMonitor(
                  librarySandboxPath = library.command,
                  targetFifoSandboxPath = relativeSandboxPath(workingDirectory, fromStrategy),
                  logPath = logPath,
                  logSandboxPath = relativeSandboxPath(workingDirectory, logPath),
                  idleLimitMs = idleLimitMs
                )
              }
            _ <- prepareInteractiveWorkspace(interactiveDir, inputPath, outputPath, statusPath, fifoPaths, input, strategyReadMonitor.toList.map(_.logPath))
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
                    limits = SandboxLimits.runtimeWithWall(
                      timeLimitMs = limits.timeMs.value.toLong,
                      wallTimeLimitMs = sharedWallTimeLimitMs,
                      memoryLimitMb = limits.memoryMb.value
                    ),
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
                args = sigpipeLauncher.args ++ interactorLauncherArgs(strategyReadMonitor, interactorCommand.command) ++ interactorArgs,
                stdin = None,
                limits = SandboxLimits.runtimeWithWall(
                  timeLimitMs = interactorLimits.timeMs.value.toLong,
                  wallTimeLimitMs = sharedWallTimeLimitMs,
                  memoryLimitMb = interactorLimits.memoryMb.value
                ),
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
                status = status,
                strategyProviderReadMonitor = strategyReadMonitor
              )
            )

        run.handleError(_ => Left(JudgeFailureReason.JudgerRuntimeFailed))

  private def prepareInteractiveWorkspace(
    interactiveDir: Path,
    inputPath: Path,
    outputPath: Path,
    statusPath: Path,
    fifoPaths: List[Path],
    input: Array[Byte],
    extraOutputPaths: List[Path] = Nil
  ): IO[Unit] =
    IO.blocking {
      Files.createDirectories(interactiveDir)
      setWorldAccessible(interactiveDir)
      Files.write(inputPath, input)
      setWorldReadable(inputPath)
      Files.deleteIfExists(outputPath)
      Files.deleteIfExists(statusPath)
      extraOutputPaths.foreach(Files.deleteIfExists)
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

  private def ensureStrategyProviderReadMonitor(config: AppConfig, workingDirectory: Path): IO[RuntimeCommand] =
    ensureCompiledLauncher(
      config = config,
      workingDirectory = workingDirectory,
      sourceName = "strategy-provider-read-monitor.cpp",
      executableName = "strategy-provider-read-monitor.so",
      source = StrategyProviderReadMonitorSource,
      extraCompilerArgs = List("-shared", "-fPIC", "-ldl", "-pthread"),
      requireExecutable = false
    )

  private def interactorLauncherArgs(monitor: Option[StrategyProviderReadMonitor], interactorCommand: String): List[String] =
    monitor match
      case None => List(interactorCommand)
      case Some(current) =>
        List(
          "--strategy-provider-read-monitor",
          current.librarySandboxPath,
          current.targetFifoSandboxPath,
          current.logSandboxPath,
          interactorCommand
        )

  private def ensureCompiledLauncher(
    config: AppConfig,
    workingDirectory: Path,
    sourceName: String,
    executableName: String,
    source: String,
    extraCompilerArgs: List[String] = Nil,
    requireExecutable: Boolean = true
  ): IO[RuntimeCommand] =
    val executablePath = workingDirectory.resolve(executableName)
    if Files.exists(executablePath) && Files.isRegularFile(executablePath) && (!requireExecutable || Files.isExecutable(executablePath)) then
      IO.pure(RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1))
    else
      resolveCompilerPath(config).flatMap {
        case Left(message) => IO.raiseError(RuntimeException(message))
        case Right(compilerPath) =>
          for
            _ <- IO.blocking(Files.writeString(workingDirectory.resolve(sourceName), source, StandardCharsets.UTF_8))
            compileResult <- runHostProcess(
              command = compilerPath,
              args = List(sourceName, "-o", executableName, "-O2") ++ extraCompilerArgs,
              cwd = workingDirectory,
              stdin = None,
              limits = SandboxLimits.runtime(timeLimitMs = 15000L, memoryLimitMb = 2048),
              stdoutName = s".$executableName.compile.stdout",
              stderrName = s".$executableName.compile.stderr"
            )
            _ <-
              if compileResult.timedOut || compileResult.exitCode.getOrElse(-1) != 0 then
                IO.raiseError(RuntimeException(s"Failed to compile $executableName launcher."))
              else if requireExecutable then ensureExecutableExists(executablePath)
              else ensureRegularFileExists(executablePath)
          yield RuntimeCommand(s"/box/$executableName", Nil, processLimit = 1)
      }

  private def ensureRegularFileExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Prepared file was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Prepared path is not a regular file: ${path.toAbsolutePath}.")
      setWorldReadable(path)
    }

  private[judger] def interactiveWallTimeLimitMs(
    testcase: JudgeTaskTestcase,
    roleCount: Int,
    interactor: JudgeTaskTool,
    strategyProvider: Option[JudgeTaskTool]
  ): Long =
    val totalCpuBudgetMs =
      testcase.limits.timeMs.value.toLong * math.max(0, roleCount).toLong +
        interactor.limits.map(_.timeMs.value.toLong).getOrElse(0L) +
        strategyProvider.flatMap(_.limits).map(_.timeMs.value.toLong).getOrElse(0L)
    (totalCpuBudgetMs * 3L + 1L) / 2L + 500L

  private def interactiveProcessesForTimeout(
    testcase: JudgeTaskTestcase,
    interactor: JudgeTaskTool,
    strategyProvider: Option[StrategyProviderRuntime],
    runResult: InteractiveRunResult
  ): List[(ProcessResult, Long)] =
    val participantProcesses =
      runResult.participants.values.toList.map(_ -> testcase.limits.timeMs.value.toLong)
    val interactorProcess =
      interactor.limits.toList.map(limits => runResult.interactor -> limits.timeMs.value.toLong)
    val strategyProcess =
      strategyProvider.toList.flatMap(provider =>
        provider.tool.limits.toList.zip(runResult.strategyProvider.toList).map { case (limits, result) =>
          result -> limits.timeMs.value.toLong
        }
      )
    participantProcesses ++ interactorProcess ++ strategyProcess

  private[judger] def interactiveToolCpuLimitExceeded(
    interactor: JudgeTaskTool,
    strategyProvider: Option[JudgeTaskTool],
    interactorResult: ProcessResult,
    strategyResult: Option[ProcessResult]
  ): Boolean =
    toolCpuLimitExceeded(interactor, interactorResult) ||
      strategyProvider.exists(provider => strategyResult.exists(result => toolCpuLimitExceeded(provider, result)))

  private[judger] def toolCpuLimitExceeded(tool: JudgeTaskTool, result: ProcessResult): Boolean =
    tool.limits.exists(limits => cpuLimitExceeded(result, limits.timeMs.value.toLong))

  private[judger] def cpuLimitExceeded(result: ProcessResult, timeLimitMs: Long): Boolean =
    result.timedOut && result.timeUsedMs.exists(_ >= timeLimitMs)

  private[judger] def interactiveWallOnlyVerdict(
    participants: Map[String, ProcessResult],
    participantCpuLimitMs: Long,
    processes: List[(ProcessResult, Long)],
    fallback: ProcessResult,
    strategyProviderReadWaitMs: Option[Long] = None,
    strategyProviderIdleLimitMs: Option[Long] = None
  ): Option[(SubmissionVerdict, ProcessResult)] =
    Option.when(interactiveWallOnlyTimeout(processes)) {
      participantFailure(participants, participantCpuLimitMs).getOrElse {
        if strategyProviderIdleLimitMs.exists(limit => strategyProviderReadWaitMs.exists(_ >= limit)) then
          SubmissionVerdict.AcceptedByProtocol -> fallback
        else SubmissionVerdict.IdlenessLimitExceeded -> fallback
      }
    }

  private[judger] def interactiveWallOnlyTimeout(processes: List[(ProcessResult, Long)]): Boolean =
    processes.exists { case (result, _) => result.timedOut } &&
      !processes.exists { case (result, timeLimitMs) => cpuLimitExceeded(result, timeLimitMs) }

  private def readStrategyProviderWaitMs(monitor: Option[StrategyProviderReadMonitor], interactorResult: ProcessResult): IO[Option[Long]] =
    monitor match
      case None => IO.pure(None)
      case Some(current) =>
        IO.blocking {
          val content =
            if Files.exists(current.logPath) && Files.isRegularFile(current.logPath) then
              Files.readString(current.logPath, StandardCharsets.UTF_8)
            else ""
          strategyProviderReadWaitMs(content, interactorResult.wallTimeUsedMs)
        }.map(Some(_)).handleError(_ => None)

  private[judger] def strategyProviderReadWaitMs(logContent: String, interactorWallTimeUsedMs: Option[Long]): Long =
    final case class Begin(seq: Long, timestampMs: Long)
    val parsedEvents =
      logContent.linesIterator.toList.flatMap { line =>
        line.trim.split("\\s+").toList match
          case "begin" :: rawSeq :: rawTimestamp :: Nil =>
            for
              seq <- rawSeq.toLongOption
              timestamp <- rawTimestamp.toLongOption
            yield Left(Begin(seq, timestamp))
          case "end" :: rawSeq :: rawTimestamp :: _ =>
            for
              seq <- rawSeq.toLongOption
              timestamp <- rawTimestamp.toLongOption
            yield Right(seq -> timestamp)
          case _ => None
      }

    val firstTimestamp =
      parsedEvents.flatMap {
        case Left(begin) => Some(begin.timestampMs)
        case Right((_, timestamp)) => Some(timestamp)
      }.minOption
    val fallbackEndMs =
      for
        first <- firstTimestamp
        wall <- interactorWallTimeUsedMs
      yield first + math.max(0L, wall)

    val (completeWaitMs, pendingBegins) =
      parsedEvents.foldLeft(0L -> Map.empty[Long, Long]) {
        case ((total, pending), Left(begin)) =>
          total -> pending.updated(begin.seq, begin.timestampMs)
        case ((total, pending), Right((seq, endMs))) =>
          pending.get(seq) match
            case Some(beginMs) => (total + math.max(0L, endMs - beginMs)) -> pending.removed(seq)
            case None => total -> pending
      }
    val pendingWaitMs =
      fallbackEndMs.toList.flatMap(endMs => pendingBegins.values.map(beginMs => math.max(0L, endMs - beginMs))).sum
    completeWaitMs + pendingWaitMs

  private def strategyFailed(strategyProvider: Option[StrategyProviderRuntime], result: Option[ProcessResult]): Boolean =
    strategyProvider.exists(_ =>
      result match
        case None => true
        case Some(current) if current.timedOut => false
        case Some(current) => current.exitCode.getOrElse(-1) != 0
    )

  private[judger] def participantFailure(participants: Map[String, ProcessResult], timeLimitMs: Long): Option[(SubmissionVerdict, ProcessResult)] =
    participants.toList.sortBy(_._1).collectFirst {
      case (_, result) if cpuLimitExceeded(result, timeLimitMs) => SubmissionVerdict.TimeLimitExceeded -> result
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
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, clampScore(score), verdict, message, reason, runResult.timeUsedMs, runResult.memoryUsedKb)

  private def testcaseSystemError(testcase: JudgeTaskTestcase, reason: JudgeFailureReason): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(reason), None, None)

  private def testcaseCompileError(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(0), SubmissionVerdict.CompileError, None, None, None, None)

  private def testcaseAcceptedByProtocol(testcase: JudgeTaskTestcase): JudgeTestcaseResult =
    JudgeTestcaseResult(testcase.index, testcase.label, testcase.testcaseType, BigDecimal(1), SubmissionVerdict.AcceptedByProtocol, None, None, Some(0L), Some(0L))

  private def subtaskCompileError(subtask: JudgeTaskSubtask): JudgeSubtaskResult =
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      score = BigDecimal(0),
      lowestScore = BigDecimal(0),
      verdict = SubmissionVerdict.CompileError,
      timeUsedMs = None,
      memoryUsedKb = None,
      reason = None,
      testcases = subtask.testcases.map(testcaseCompileError),
      baseResult = None
    )

  private def subtaskSystemError(subtask: JudgeTaskSubtask, reason: JudgeFailureReason): JudgeSubtaskResult =
    JudgeSubtaskResult(
      index = subtask.index,
      label = subtask.label,
      score = BigDecimal(0),
      lowestScore = BigDecimal(0),
      verdict = SubmissionVerdict.SystemError,
      timeUsedMs = None,
      memoryUsedKb = None,
      reason = Some(reason),
      testcases = subtask.testcases.map(testcase => testcaseSystemError(testcase, reason)),
      baseResult = None
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

  private[infra] def aggregateSubtask(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): JudgeSubtaskResult =
    val result = aggregateSubtaskWithoutBase(subtask, testcases)
    val baseResult =
      Option.when(testcases.exists(_.testcaseType == JudgeTestcaseType.Hack)) {
        aggregateSubtaskWithoutBase(subtask, testcases.filterNot(_.testcaseType == JudgeTestcaseType.Hack))
      }
    result.copy(baseResult = baseResult)

  private def aggregateSubtaskWithoutBase(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): JudgeSubtaskResult =
    val scoreAggregation = aggregateTestcaseScores(subtask, testcases)
    val score = scoreAggregation.score
    val lowestScore = scoreAggregation.lowestScore
    val verdict = aggregateVerdict(score, scoreAggregation.verdictChildren)
    val reason = Option.when(verdict == SubmissionVerdict.SystemError)(
      testcases.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )
    JudgeSubtaskResult(
      subtask.index,
      subtask.label,
      score,
      lowestScore,
      verdict,
      aggregateUsage(subtask.aggregation.time, testcases.flatMap(_.timeUsedMs)),
      aggregateUsage(subtask.aggregation.memory, testcases.flatMap(_.memoryUsedKb)),
      reason,
      testcases,
      baseResult = None
    )

  private[infra] def aggregateTask(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): JudgeResult =
    val result = aggregateTaskWithoutBase(task, subtasks)
    val baseResult =
      Option.when(subtasks.exists(_.baseResult.nonEmpty)) {
        aggregateTaskWithoutBase(task, subtasks.map(subtask => subtask.baseResult.getOrElse(subtask.copy(baseResult = None))))
      }
    result.copy(baseResult = baseResult)

  private def aggregateTaskWithoutBase(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): JudgeResult =
    val scoreAggregation = aggregateSubtaskScores(task, subtasks)
    val rawScore = scoreAggregation.score
    val score = roundFinalScore(rawScore, task.roundingScale)
    val lowestScore = roundFinalScore(scoreAggregation.lowestScore, task.roundingScale)
    val verdict = aggregateVerdict(score, scoreAggregation.verdictChildren)
    val reason = Option.when(verdict == SubmissionVerdict.SystemError)(
      subtasks.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )
    JudgeResult(
      score,
      lowestScore,
      verdict,
      reason,
      aggregateUsage(task.aggregation.time, subtasks.flatMap(_.timeUsedMs)),
      aggregateUsage(task.aggregation.memory, subtasks.flatMap(_.memoryUsedKb)),
      subtasks,
      baseResult = None
    )

  private def aggregateTestcaseScores(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): ScoreAggregation =
    val (hackTestcases, baseTestcases) = testcases.partition(_.testcaseType == JudgeTestcaseType.Hack)
    val allLowestScore = lowestScore(testcases.map(result => result.score -> result.verdict))
    if subtask.aggregation.score == "sum" && hackTestcases.nonEmpty then
      val baseCandidate =
        Option.when(baseTestcases.nonEmpty) {
          val baseScore = aggregateScore("sum", baseTestcases.map(_.score), testcaseScoreRatios(subtask, baseTestcases))
          val baseVerdict = aggregateVerdict(baseScore, baseTestcases.map(result => result.score -> result.verdict))
          baseScore -> baseVerdict
        }
      val scoreChildren = baseCandidate.toList ++ hackTestcases.map(result => result.score -> result.verdict)
      ScoreAggregation(lowestScore(scoreChildren), allLowestScore, scoreChildren)
    else
      val score = aggregateScore(subtask.aggregation.score, testcases.map(_.score), testcaseScoreRatios(subtask, testcases))
      ScoreAggregation(score, allLowestScore, testcases.map(result => result.score -> result.verdict))

  private def aggregateSubtaskScores(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): ScoreAggregation =
    val hackTestcaseChildren =
      subtasks.flatMap(_.testcases.collect {
        case testcase if testcase.testcaseType == JudgeTestcaseType.Hack => testcase.score -> testcase.verdict
      })
    if task.aggregation.score == "sum" && hackTestcaseChildren.nonEmpty then
      val baseSubtasks = subtasks.map(subtask => subtask.baseResult.getOrElse(subtask.copy(baseResult = None)))
      val baseCandidate =
        Option.when(baseSubtasks.exists(_.testcases.nonEmpty)) {
          val baseScore = aggregateScore("sum", baseSubtasks.map(_.score), task.subtasks.map(_.scoreRatio))
          val baseVerdict = aggregateVerdict(roundFinalScore(baseScore, task.roundingScale), baseSubtasks.map(result => result.score -> result.verdict))
          baseScore -> baseVerdict
        }
      val baseLowestScore =
        Option.when(baseSubtasks.exists(_.testcases.nonEmpty)) {
          aggregateScore("sum", baseSubtasks.map(_.lowestScore), task.subtasks.map(_.scoreRatio))
        }
      val scoreChildren = baseCandidate.toList ++ hackTestcaseChildren
      val lowestScoreChildren = baseLowestScore.toList.map(score => score -> SubmissionVerdict.Accepted) ++ hackTestcaseChildren
      ScoreAggregation(lowestScore(scoreChildren), lowestScore(lowestScoreChildren), scoreChildren)
    else
      val score = aggregateScore(task.aggregation.score, subtasks.map(_.score), task.subtasks.map(_.scoreRatio))
      val lowest = aggregateScore(task.aggregation.score, subtasks.map(_.lowestScore), task.subtasks.map(_.scoreRatio))
      ScoreAggregation(score, lowest, subtasks.map(result => result.score -> result.verdict))

  private def testcaseScoreRatios(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): List[BigDecimal] =
    testcases.map(result => subtask.testcases.find(_.index == result.index).map(_.scoreRatio).getOrElse(BigDecimal(1)))

  private def lowestScore(children: List[(BigDecimal, SubmissionVerdict)]): BigDecimal =
    children.map(_._1).minOption.getOrElse(BigDecimal(0))

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
      |#include <stddef.h>
      |#include <stdlib.h>
      |#include <string.h>
      |#include <unistd.h>
      |
      |static int prepend_ld_preload(const char* library_path) {
      |  const char* existing = getenv("LD_PRELOAD");
      |  if (existing == NULL || existing[0] == '\0') {
      |    return setenv("LD_PRELOAD", library_path, 1);
      |  }
      |
      |  size_t library_len = strlen(library_path);
      |  size_t existing_len = strlen(existing);
      |  char* combined = (char*)malloc(library_len + 1 + existing_len + 1);
      |  if (combined == NULL) {
      |    return -1;
      |  }
      |
      |  memcpy(combined, library_path, library_len);
      |  combined[library_len] = ':';
      |  memcpy(combined + library_len + 1, existing, existing_len + 1);
      |  int result = setenv("LD_PRELOAD", combined, 1);
      |  free(combined);
      |  return result;
      |}
      |
      |int main(int argc, char** argv) {
      |  if (argc < 2) {
      |    return 127;
      |  }
      |
      |  int command_index = 1;
      |  if (strcmp(argv[1], "--strategy-provider-read-monitor") == 0) {
      |    if (argc < 6) {
      |      return 127;
      |    }
      |    if (prepend_ld_preload(argv[2]) != 0 ||
      |        setenv("QIWEN_STRATEGY_PROVIDER_READ_FIFO", argv[3], 1) != 0 ||
      |        setenv("QIWEN_STRATEGY_PROVIDER_READ_LOG", argv[4], 1) != 0) {
      |      return 127;
      |    }
      |    command_index = 5;
      |  }
      |
      |  if (signal(SIGPIPE, SIG_IGN) == SIG_ERR) {
      |    return 127;
      |  }
      |  execvp(argv[command_index], argv + command_index);
      |  perror("execvp");
      |  return 127;
      |}
      |""".stripMargin

  private val StrategyProviderReadMonitorSource: String =
    """#define _GNU_SOURCE
      |#include <dlfcn.h>
      |#include <errno.h>
      |#include <fcntl.h>
      |#include <limits.h>
      |#include <pthread.h>
      |#include <stdarg.h>
      |#include <stddef.h>
      |#include <stdint.h>
      |#include <stdio.h>
      |#include <stdlib.h>
      |#include <string.h>
      |#include <sys/types.h>
      |#include <sys/uio.h>
      |#include <time.h>
      |#include <unistd.h>
      |
      |typedef int (*open_fn_t)(const char*, int, ...);
      |typedef int (*openat_fn_t)(int, const char*, int, ...);
      |typedef ssize_t (*read_fn_t)(int, void*, size_t);
      |typedef ssize_t (*readv_fn_t)(int, const struct iovec*, int);
      |typedef int (*close_fn_t)(int);
      |typedef FILE* (*fopen_fn_t)(const char*, const char*);
      |typedef int (*fclose_fn_t)(FILE*);
      |typedef size_t (*fread_fn_t)(void*, size_t, size_t, FILE*);
      |typedef char* (*fgets_fn_t)(char*, int, FILE*);
      |typedef int (*fgetc_fn_t)(FILE*);
      |typedef ssize_t (*getline_fn_t)(char**, size_t*, FILE*);
      |typedef ssize_t (*getdelim_fn_t)(char**, size_t*, int, FILE*);
      |typedef int (*vfscanf_fn_t)(FILE*, const char*, va_list);
      |
      |static pthread_once_t symbols_once = PTHREAD_ONCE_INIT;
      |static pthread_once_t config_once = PTHREAD_ONCE_INIT;
      |static pthread_mutex_t state_mutex = PTHREAD_MUTEX_INITIALIZER;
      |static pthread_mutex_t log_mutex = PTHREAD_MUTEX_INITIALIZER;
      |
      |static open_fn_t real_open_fn = NULL;
      |static open_fn_t real_open64_fn = NULL;
      |static openat_fn_t real_openat_fn = NULL;
      |static openat_fn_t real_openat64_fn = NULL;
      |static read_fn_t real_read_fn = NULL;
      |static read_fn_t real___read_fn = NULL;
      |static read_fn_t real___libc_read_fn = NULL;
      |static readv_fn_t real_readv_fn = NULL;
      |static close_fn_t real_close_fn = NULL;
      |static fopen_fn_t real_fopen_fn = NULL;
      |static fopen_fn_t real_fopen64_fn = NULL;
      |static fclose_fn_t real_fclose_fn = NULL;
      |static fread_fn_t real_fread_fn = NULL;
      |static fread_fn_t real_fread_unlocked_fn = NULL;
      |static fgets_fn_t real_fgets_fn = NULL;
      |static fgetc_fn_t real_fgetc_fn = NULL;
      |static getline_fn_t real_getline_fn = NULL;
      |static getdelim_fn_t real_getdelim_fn = NULL;
      |static vfscanf_fn_t real_vfscanf_fn = NULL;
      |static vfscanf_fn_t real___isoc99_vfscanf_fn = NULL;
      |
      |static char tracked_fds[4096];
      |static FILE* tracked_files[1024];
      |static char target_raw[PATH_MAX];
      |static char target_real[PATH_MAX];
      |static char log_path[PATH_MAX];
      |static int monitor_enabled = 0;
      |static unsigned long long next_seq = 1;
      |static __thread int in_monitor = 0;
      |
      |static void load_symbols(void) {
      |  real_open_fn = (open_fn_t)dlsym(RTLD_NEXT, "open");
      |  real_open64_fn = (open_fn_t)dlsym(RTLD_NEXT, "open64");
      |  real_openat_fn = (openat_fn_t)dlsym(RTLD_NEXT, "openat");
      |  real_openat64_fn = (openat_fn_t)dlsym(RTLD_NEXT, "openat64");
      |  real_read_fn = (read_fn_t)dlsym(RTLD_NEXT, "read");
      |  real___read_fn = (read_fn_t)dlsym(RTLD_NEXT, "__read");
      |  real___libc_read_fn = (read_fn_t)dlsym(RTLD_NEXT, "__libc_read");
      |  real_readv_fn = (readv_fn_t)dlsym(RTLD_NEXT, "readv");
      |  real_close_fn = (close_fn_t)dlsym(RTLD_NEXT, "close");
      |  real_fopen_fn = (fopen_fn_t)dlsym(RTLD_NEXT, "fopen");
      |  real_fopen64_fn = (fopen_fn_t)dlsym(RTLD_NEXT, "fopen64");
      |  real_fclose_fn = (fclose_fn_t)dlsym(RTLD_NEXT, "fclose");
      |  real_fread_fn = (fread_fn_t)dlsym(RTLD_NEXT, "fread");
      |  real_fread_unlocked_fn = (fread_fn_t)dlsym(RTLD_NEXT, "fread_unlocked");
      |  real_fgets_fn = (fgets_fn_t)dlsym(RTLD_NEXT, "fgets");
      |  real_fgetc_fn = (fgetc_fn_t)dlsym(RTLD_NEXT, "fgetc");
      |  real_getline_fn = (getline_fn_t)dlsym(RTLD_NEXT, "getline");
      |  real_getdelim_fn = (getdelim_fn_t)dlsym(RTLD_NEXT, "getdelim");
      |  real_vfscanf_fn = (vfscanf_fn_t)dlsym(RTLD_NEXT, "vfscanf");
      |  real___isoc99_vfscanf_fn = (vfscanf_fn_t)dlsym(RTLD_NEXT, "__isoc99_vfscanf");
      |
      |  if (real_open64_fn == NULL) real_open64_fn = real_open_fn;
      |  if (real_openat64_fn == NULL) real_openat64_fn = real_openat_fn;
      |  if (real___read_fn == NULL) real___read_fn = real_read_fn;
      |  if (real___libc_read_fn == NULL) real___libc_read_fn = real_read_fn;
      |  if (real_fopen64_fn == NULL) real_fopen64_fn = real_fopen_fn;
      |  if (real_fread_unlocked_fn == NULL) real_fread_unlocked_fn = real_fread_fn;
      |  if (real___isoc99_vfscanf_fn == NULL) real___isoc99_vfscanf_fn = real_vfscanf_fn;
      |}
      |
      |static void load_config(void) {
      |  const char* target = getenv("QIWEN_STRATEGY_PROVIDER_READ_FIFO");
      |  const char* log = getenv("QIWEN_STRATEGY_PROVIDER_READ_LOG");
      |  if (target == NULL || target[0] == '\0' || log == NULL || log[0] == '\0') {
      |    return;
      |  }
      |
      |  snprintf(target_raw, sizeof(target_raw), "%s", target);
      |  snprintf(log_path, sizeof(log_path), "%s", log);
      |  int previous_in_monitor = in_monitor;
      |  in_monitor = 1;
      |  if (realpath(target, target_real) == NULL) {
      |    target_real[0] = '\0';
      |  }
      |  in_monitor = previous_in_monitor;
      |  monitor_enabled = 1;
      |}
      |
      |static int needs_open_mode(int flags) {
      |#ifdef O_TMPFILE
      |  return (flags & O_CREAT) != 0 || ((flags & O_TMPFILE) == O_TMPFILE);
      |#else
      |  return (flags & O_CREAT) != 0;
      |#endif
      |}
      |
      |static int resolve_path(int dirfd, const char* path, char* out, size_t out_size) {
      |  if (path == NULL || path[0] == '\0') {
      |    return 0;
      |  }
      |
      |  char candidate[PATH_MAX];
      |  if (path[0] == '/') {
      |    snprintf(candidate, sizeof(candidate), "%s", path);
      |  } else {
      |    char base[PATH_MAX];
      |    if (dirfd == AT_FDCWD) {
      |      if (getcwd(base, sizeof(base)) == NULL) {
      |        return 0;
      |    }
      |    } else {
      |      char proc_path[64];
      |      snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", dirfd);
      |      ssize_t length = readlink(proc_path, base, sizeof(base) - 1);
      |      if (length < 0) {
      |        return 0;
      |      }
      |      base[length] = '\0';
      |    }
      |    snprintf(candidate, sizeof(candidate), "%s/%s", base, path);
      |  }
      |
      |  int previous_in_monitor = in_monitor;
      |  in_monitor = 1;
      |  char* resolved = realpath(candidate, out);
      |  in_monitor = previous_in_monitor;
      |  if (resolved != NULL) {
      |    return 1;
      |  }
      |  snprintf(out, out_size, "%s", candidate);
      |  return 1;
      |}
      |
      |static int path_matches_target(int dirfd, const char* path) {
      |  pthread_once(&config_once, load_config);
      |  if (!monitor_enabled || path == NULL) {
      |    return 0;
      |  }
      |  if (strcmp(path, target_raw) == 0) {
      |    return 1;
      |  }
      |
      |  char resolved[PATH_MAX];
      |  if (target_real[0] != '\0' && resolve_path(dirfd, path, resolved, sizeof(resolved))) {
      |    return strcmp(resolved, target_real) == 0;
      |  }
      |  return 0;
      |}
      |
      |static long long monotonic_ms(void) {
      |  struct timespec current;
      |  if (clock_gettime(CLOCK_MONOTONIC, &current) != 0) {
      |    return 0;
      |  }
      |  return (long long)current.tv_sec * 1000LL + current.tv_nsec / 1000000LL;
      |}
      |
      |static void log_line(const char* line) {
      |  pthread_once(&symbols_once, load_symbols);
      |  pthread_once(&config_once, load_config);
      |  if (!monitor_enabled || real_open_fn == NULL || real_close_fn == NULL) {
      |    return;
      |  }
      |
      |  pthread_mutex_lock(&log_mutex);
      |  in_monitor = 1;
      |  int fd = real_open_fn(log_path, O_WRONLY | O_CREAT | O_APPEND | O_CLOEXEC, 0666);
      |  if (fd >= 0) {
      |    size_t length = strlen(line);
      |    const char* current = line;
      |    while (length > 0) {
      |      ssize_t written = write(fd, current, length);
      |      if (written < 0) {
      |        if (errno == EINTR) {
      |          continue;
      |        }
      |        break;
      |      }
      |      current += written;
      |      length -= (size_t)written;
      |    }
      |    real_close_fn(fd);
      |  }
      |  in_monitor = 0;
      |  pthread_mutex_unlock(&log_mutex);
      |}
      |
      |static unsigned long long log_begin(void) {
      |  unsigned long long seq = __sync_fetch_and_add(&next_seq, 1);
      |  char line[160];
      |  snprintf(line, sizeof(line), "begin %llu %lld\n", seq, monotonic_ms());
      |  log_line(line);
      |  return seq;
      |}
      |
      |static void log_end(unsigned long long seq, long long result) {
      |  char line[160];
      |  snprintf(line, sizeof(line), "end %llu %lld %lld\n", seq, monotonic_ms(), result);
      |  log_line(line);
      |}
      |
      |static int is_tracked_fd(int fd) {
      |  int result = 0;
      |  if (fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    pthread_mutex_lock(&state_mutex);
      |    result = tracked_fds[fd] != 0;
      |    pthread_mutex_unlock(&state_mutex);
      |  }
      |  return result;
      |}
      |
      |static void track_fd_if_target(int fd, int dirfd, const char* path) {
      |  if (fd < 0 || fd >= (int)sizeof(tracked_fds) || in_monitor || !path_matches_target(dirfd, path)) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  tracked_fds[fd] = 1;
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static void untrack_fd(int fd) {
      |  if (fd < 0 || fd >= (int)sizeof(tracked_fds)) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  tracked_fds[fd] = 0;
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static void track_file(FILE* stream) {
      |  if (stream == NULL) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  for (size_t i = 0; i < sizeof(tracked_files) / sizeof(tracked_files[0]); ++i) {
      |    if (tracked_files[i] == NULL || tracked_files[i] == stream) {
      |      tracked_files[i] = stream;
      |      break;
      |    }
      |  }
      |  int fd = fileno(stream);
      |  if (fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    tracked_fds[fd] = 1;
      |  }
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static void untrack_file(FILE* stream) {
      |  if (stream == NULL) {
      |    return;
      |  }
      |  pthread_mutex_lock(&state_mutex);
      |  for (size_t i = 0; i < sizeof(tracked_files) / sizeof(tracked_files[0]); ++i) {
      |    if (tracked_files[i] == stream) {
      |      tracked_files[i] = NULL;
      |    }
      |  }
      |  int fd = fileno(stream);
      |  if (fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    tracked_fds[fd] = 0;
      |  }
      |  pthread_mutex_unlock(&state_mutex);
      |}
      |
      |static int is_tracked_file(FILE* stream) {
      |  if (stream == NULL) {
      |    return 0;
      |  }
      |  int fd = fileno(stream);
      |  int result = 0;
      |  pthread_mutex_lock(&state_mutex);
      |  for (size_t i = 0; i < sizeof(tracked_files) / sizeof(tracked_files[0]); ++i) {
      |    if (tracked_files[i] == stream) {
      |      result = 1;
      |      break;
      |    }
      |  }
      |  if (!result && fd >= 0 && fd < (int)sizeof(tracked_fds)) {
      |    result = tracked_fds[fd] != 0;
      |  }
      |  pthread_mutex_unlock(&state_mutex);
      |  return result;
      |}
      |
      |static ssize_t monitored_read(read_fn_t real_fn, int fd, void* buffer, size_t count) {
      |  if (real_fn == NULL) {
      |    errno = ENOSYS;
      |    return -1;
      |  }
      |  if (in_monitor || !is_tracked_fd(fd)) {
      |    return real_fn(fd, buffer, count);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_fn(fd, buffer, count);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int open(const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_open_fn(path, flags, mode) : real_open_fn(path, flags);
      |  track_fd_if_target(fd, AT_FDCWD, path);
      |  return fd;
      |}
      |
      |extern "C" int open64(const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_open64_fn(path, flags, mode) : real_open64_fn(path, flags);
      |  track_fd_if_target(fd, AT_FDCWD, path);
      |  return fd;
      |}
      |
      |extern "C" int openat(int dirfd, const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_openat_fn(dirfd, path, flags, mode) : real_openat_fn(dirfd, path, flags);
      |  track_fd_if_target(fd, dirfd, path);
      |  return fd;
      |}
      |
      |extern "C" int openat64(int dirfd, const char* path, int flags, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  mode_t mode = 0;
      |  if (needs_open_mode(flags)) {
      |    va_list args;
      |    va_start(args, flags);
      |    mode = (mode_t)va_arg(args, int);
      |    va_end(args);
      |  }
      |  int fd = needs_open_mode(flags) ? real_openat64_fn(dirfd, path, flags, mode) : real_openat64_fn(dirfd, path, flags);
      |  track_fd_if_target(fd, dirfd, path);
      |  return fd;
      |}
      |
      |extern "C" ssize_t read(int fd, void* buffer, size_t count) {
      |  pthread_once(&symbols_once, load_symbols);
      |  return monitored_read(real_read_fn, fd, buffer, count);
      |}
      |
      |extern "C" ssize_t __read(int fd, void* buffer, size_t count) {
      |  pthread_once(&symbols_once, load_symbols);
      |  return monitored_read(real___read_fn, fd, buffer, count);
      |}
      |
      |extern "C" ssize_t __libc_read(int fd, void* buffer, size_t count) {
      |  pthread_once(&symbols_once, load_symbols);
      |  return monitored_read(real___libc_read_fn, fd, buffer, count);
      |}
      |
      |extern "C" ssize_t readv(int fd, const struct iovec* iov, int iovcnt) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (real_readv_fn == NULL) {
      |    errno = ENOSYS;
      |    return -1;
      |  }
      |  if (in_monitor || !is_tracked_fd(fd)) {
      |    return real_readv_fn(fd, iov, iovcnt);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_readv_fn(fd, iov, iovcnt);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int close(int fd) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (!in_monitor) {
      |    untrack_fd(fd);
      |  }
      |  return real_close_fn(fd);
      |}
      |
      |extern "C" FILE* fopen(const char* path, const char* mode) {
      |  pthread_once(&symbols_once, load_symbols);
      |  FILE* stream = real_fopen_fn(path, mode);
      |  if (!in_monitor && stream != NULL && path_matches_target(AT_FDCWD, path)) {
      |    track_file(stream);
      |  }
      |  return stream;
      |}
      |
      |extern "C" FILE* fopen64(const char* path, const char* mode) {
      |  pthread_once(&symbols_once, load_symbols);
      |  FILE* stream = real_fopen64_fn(path, mode);
      |  if (!in_monitor && stream != NULL && path_matches_target(AT_FDCWD, path)) {
      |    track_file(stream);
      |  }
      |  return stream;
      |}
      |
      |extern "C" int fclose(FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (!in_monitor) {
      |    untrack_file(stream);
      |  }
      |  return real_fclose_fn(stream);
      |}
      |
      |extern "C" size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fread_fn(ptr, size, nmemb, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  size_t result = real_fread_fn(ptr, size, nmemb, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" size_t fread_unlocked(void* ptr, size_t size, size_t nmemb, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fread_unlocked_fn(ptr, size, nmemb, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  size_t result = real_fread_unlocked_fn(ptr, size, nmemb, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" char* fgets(char* s, int size, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fgets_fn(s, size, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  char* result = real_fgets_fn(s, size, stream);
      |  log_end(seq, result == NULL ? 0LL : 1LL);
      |  return result;
      |}
      |
      |extern "C" int fgetc(FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_fgetc_fn(stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  int result = real_fgetc_fn(stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" ssize_t getline(char** lineptr, size_t* n, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_getline_fn(lineptr, n, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_getline_fn(lineptr, n, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" ssize_t getdelim(char** lineptr, size_t* n, int delim, FILE* stream) {
      |  pthread_once(&symbols_once, load_symbols);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    return real_getdelim_fn(lineptr, n, delim, stream);
      |  }
      |  unsigned long long seq = log_begin();
      |  ssize_t result = real_getdelim_fn(lineptr, n, delim, stream);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int monitored_fscanf(FILE* stream, const char* format, ...) __asm__("fscanf");
      |extern "C" int monitored_isoc99_fscanf(FILE* stream, const char* format, ...) __asm__("__isoc99_fscanf");
      |
      |extern "C" int monitored_fscanf(FILE* stream, const char* format, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  va_list args;
      |  va_start(args, format);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    int result = real_vfscanf_fn(stream, format, args);
      |    va_end(args);
      |    return result;
      |  }
      |  unsigned long long seq = log_begin();
      |  int result = real_vfscanf_fn(stream, format, args);
      |  va_end(args);
      |  log_end(seq, (long long)result);
      |  return result;
      |}
      |
      |extern "C" int monitored_isoc99_fscanf(FILE* stream, const char* format, ...) {
      |  pthread_once(&symbols_once, load_symbols);
      |  va_list args;
      |  va_start(args, format);
      |  if (in_monitor || !is_tracked_file(stream)) {
      |    int result = real___isoc99_vfscanf_fn(stream, format, args);
      |    va_end(args);
      |    return result;
      |  }
      |  unsigned long long seq = log_begin();
      |  int result = real___isoc99_vfscanf_fn(stream, format, args);
      |  va_end(args);
      |  log_end(seq, (long long)result);
      |  return result;
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
