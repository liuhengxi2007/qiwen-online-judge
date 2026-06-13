package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode, SubmissionVerdict}
import judgeprotocol.objects.request.ReportHackResultRequest
import judgeprotocol.objects.response.*
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.infra.JudgeTestcaseResults.*
import judger.objects.{SandboxLimits, SandboxRunSpec, SandboxStdin, SandboxStdout}

import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** hack 尝试执行器，负责校验输入、生成可选答案、运行目标提交并重算结果。 */
object HackAttemptRunner:
  /** 执行完整 hack 流程；失败会转成 ReportHackResultRequest，而不是向上抛出业务异常。 */
  def hack(
    task: HackTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportHackResultRequest] =
    withWorkingDirectory(config.workRoot, "qiwen-hack-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        JudgeToolPreparation.preparePrograms(task.targetTask, config, workingDirectory, problemDataCache, runtimes).flatMap {
          case Left(result) =>
            IO.pure(hackFailed(task, result.judgeResult.flatMap(_.baseResult.reason).map(JudgeFailureReason.render).getOrElse("Target prepare failed.")))
          case Right(programs) =>
            JudgeToolPreparation.prepareTools(task.targetTask, config, workingDirectory, problemDataCache).flatMap {
              case Left(result) =>
                IO.pure(hackFailed(task, result.judgeResult.flatMap(_.baseResult.reason).map(JudgeFailureReason.render).getOrElse("Tool prepare failed.")))
              case Right(tools) =>
                executeHack(task, config, workingDirectory, sandbox, problemDataCache, runtimes, programs, tools)
            }
        }
      }
    }.handleError { error =>
      hackFailed(task, Option(error.getMessage).getOrElse("Hack execution failed."))
    }

  /** 执行单个已准备好的 hack 任务，按 validator、答案生成、目标运行的顺序推进。 */
  private def executeHack(
    hackTask: HackTask,
    config: AppConfig,
    workingDirectory: Path,
    sandbox: SandboxSession,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime],
    programs: JudgeToolPreparation.PreparedPrograms,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[ReportHackResultRequest] =
    val task = hackTask.targetTask
    task.subtasks.find(_.index == hackTask.subtaskIndex) match
      case None =>
        IO.pure(hackFailed(hackTask, "Target subtask was not found."))
      case Some(subtask) =>
        val input = hackTask.input.getBytes(StandardCharsets.UTF_8)
        val oldWorstScore = targetSubtaskWorstScore(hackTask)
        if !subtask.hack.enabled then IO.pure(hackFailed(hackTask, "Hack is disabled for this subtask."))
        else
          subtask.validator match
            case None => IO.pure(hackFailed(hackTask, "Target subtask has no validator."))
            case Some(validator) =>
              runValidator(hackTask, subtask, input, workingDirectory, sandbox, validator, tools).flatMap {
                case Some(message) =>
                  // 注意：hack status 字符串与 backend HackStatus 协议值手工同步，当前 judger 只按协议字面值回报。
                  IO.pure(
                    ReportHackResultRequest(
                      status = "invalid",
                      answer = None,
                      oldScore = oldWorstScore,
                      newScore = None,
                      newResult = None,
                      validatorMessage = Some(message),
                      standardMessage = None,
                      targetMessage = None
                    )
                  )
                case None =>
                  prepareHackAnswer(hackTask, subtask, input, config, workingDirectory, sandbox, problemDataCache, runtimes).flatMap {
                    case Left(message) => IO.pure(hackFailed(hackTask, message, standardMessage = Some(message)))
                    case Right(answerBytes) =>
                      runTargetOnHack(hackTask, subtask, input, answerBytes, config, workingDirectory, sandbox, programs, tools).map { testcaseResult =>
                        val newSubtask = appendHackTestcaseResult(subtask, hackTask.oldResult.subtasks, testcaseResult)
                        val newSubtasks = replaceSubtaskResult(hackTask.oldResult.subtasks, newSubtask)
                        val newResult = JudgeResultAggregator.aggregateTask(task, newSubtasks)
                        val targetMessage = Some(s"${SubmissionVerdict.render(testcaseResult.verdict)} score=${testcaseResult.score}")
                        val status = if newSubtask.worstResult.score < oldWorstScore then "success" else "no_effect"
                        ReportHackResultRequest(
                          status = status,
                          answer = answerBytes.map(answer => new String(answer, StandardCharsets.UTF_8)),
                          oldScore = oldWorstScore,
                          newScore = Some(newSubtask.worstResult.score),
                          newResult = Some(newResult),
                          validatorMessage = Some("accepted"),
                          standardMessage = answerBytes.fold(Some("not_required"))(_ => Some("accepted")),
                          targetMessage = targetMessage
                        )
                      }
                  }
              }

  /** 运行子任务 validator；返回 Some(message) 表示 hack 输入无效。 */
  private def runValidator(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    workingDirectory: Path,
    sandbox: SandboxSession,
    validator: JudgeTaskTool,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[Option[String]] =
    tools.validators.get(validator.source.path) match
      case None => IO.pure(Some("Validator could not be prepared."))
      case Some(command) =>
        val limits = validator.limits
          .map(limits => SandboxLimits.runtime(limits.timeMs.value.toLong, limits.memoryMb.value))
          // 注意：validator 没有显式 limits 时使用保守默认值，兼容旧题目配置。
          .getOrElse(SandboxLimits.runtime(timeLimitMs = 5000L, memoryLimitMb = 512))
        sandbox
          .run(
            SandboxRunSpec(
              phase = s"hack-validator-${hackTask.hackId}-${subtask.index}",
              command = command,
              stdin = SandboxStdin.Bytes(input),
              stdout = SandboxStdout.Capture,
              limits = limits
            ),
            workingDirectory
          )
          .map { result =>
            if !result.timedOut && result.exitCode.contains(0) then None
            else Some(renderDetail("Validator rejected the hack input.", result))
          }

  /** 根据子任务 hack 配置决定是否运行标准程序生成答案。 */
  private def prepareHackAnswer(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    config: AppConfig,
    workingDirectory: Path,
    sandbox: SandboxSession,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[Either[String, Option[Array[Byte]]]] =
    if !subtask.hack.enabled then IO.pure(Left("Hack is disabled for this subtask."))
    else
      subtask.hack.answerGeneration match
        case JudgeTaskHackConfig.NoAnswerGeneration => IO.pure(Right(None))
        case JudgeTaskHackConfig.StandardAnswerGeneration =>
          (subtask.standard, templateHackTestcase(subtask)) match
            case (None, _) => IO.pure(Left("Target subtask has no answer generator."))
            case (_, None) => IO.pure(Left("Target subtask has no testcase template."))
            case (Some(standard), Some(template)) =>
              runAnswerGenerator(hackTask, subtask, input, config, workingDirectory, sandbox, problemDataCache, runtimes, standard, template).map(_.map(Some(_)))
        case other => IO.pure(Left(s"Unsupported hack answer generation mode: $other."))

  /** 编译并运行答案生成器；输出 stdout 字节作为 hack 答案。 */
  private def runAnswerGenerator(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    config: AppConfig,
    workingDirectory: Path,
    sandbox: SandboxSession,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime],
    standard: JudgeTaskStandard,
    template: JudgeTaskTestcase
  ): IO[Either[String, Array[Byte]]] =
    runtimes.get(standard.language) match
      case None => IO.pure(Left("Answer generator language is not supported by this judger."))
      case Some(runtime) =>
        problemDataCache.loadBytes(hackTask.targetTask.problemSlug, hackTask.targetTask.problemDataVersion, standard.source).attempt.flatMap {
          case Left(error) => IO.pure(Left(s"Answer generator source could not be loaded: ${Option(error.getMessage).getOrElse(error.getClass.getName)}"))
          case Right(sourceBytes) =>
            val sourceCode = SubmissionSourceCode(new String(sourceBytes, StandardCharsets.UTF_8))
            runtime.prepare(s"standard-${hackTask.hackId}-${subtask.index}", sourceCode, None, Nil, config, workingDirectory).flatMap {
              case Left(ProgramPrepareFailure.CompileError) => IO.pure(Left("Answer generator failed to compile."))
              case Left(ProgramPrepareFailure.SystemError(reason)) => IO.pure(Left(s"Answer generator failed to prepare: ${JudgeFailureReason.render(reason)}."))
              case Right(command) =>
                sandbox
                  .run(
                    SandboxRunSpec(
                      phase = s"hack-standard-${hackTask.hackId}-${subtask.index}",
                      command = command,
                      stdin = SandboxStdin.Bytes(input),
                      stdout = SandboxStdout.Capture,
                      limits = SandboxLimits.runtime(template.limits.timeMs.value.toLong, template.limits.memoryMb.value)
                    ),
                    workingDirectory
                  )
                  .map { result =>
                    if result.timedOut then Left(renderDetail("Answer generator timed out.", result))
                    else if result.exitCode.getOrElse(-1) != 0 then Left(renderDetail("Answer generator failed at runtime.", result))
                    else Right(result.stdout.getBytes(StandardCharsets.UTF_8))
                  }
            }
        }

  /** 用 hack 输入和可选答案运行目标提交，并返回新增 hack 测试点结果。 */
  private def runTargetOnHack(
    hackTask: HackTask,
    subtask: JudgeTaskSubtask,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    config: AppConfig,
    workingDirectory: Path,
    sandbox: SandboxSession,
    programs: JudgeToolPreparation.PreparedPrograms,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeTestcaseResult] =
    templateHackTestcase(subtask) match
      case None =>
        // FIXME-CN: 缺少模板测试点时硬编码 index=1/label=hack，可能与原子任务结果树索引冲突或隐藏配置构建错误。
        IO.pure(JudgeTestcaseResult(1, Some("hack"), JudgeTestcaseType.Hack, BigDecimal(0), SubmissionVerdict.SystemError, None, Some(JudgeFailureReason.SystemError), None, None))
      case Some(testcase) =>
        subtask.mode.`type` match
          case "traditional" =>
            val selection = TraditionalProgramSelector.select(hackTask.targetTask, subtask, testcase, programs)
            TraditionalTestcaseRunner.runData(hackTask.targetTask, subtask.index, testcase, workingDirectory, sandbox, input, answerBytes, selection, tools)
          case "interactive" =>
            runInteractiveHackTestcase(hackTask, config, subtask, testcase, input, answerBytes, workingDirectory, sandbox, programs, tools)
          case _ =>
            // FIXME-CN: 未知 mode.type 在 hack 路径同样折叠为 SystemError，无法暴露协议扩展或配置拼写错误。
            IO.pure(testcaseSystemError(testcase, JudgeFailureReason.SystemError))

  /** 在交互题子任务上运行 hack 输入，必要时编译本次 hack 提供的策略 provider。 */
  private def runInteractiveHackTestcase(
    hackTask: HackTask,
    config: AppConfig,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    workingDirectory: Path,
    sandbox: SandboxSession,
    programs: JudgeToolPreparation.PreparedPrograms,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeTestcaseResult] =
    val missingRole = subtask.mode.roles.find(role => SubmissionJudgeRunner.programCommand(hackTask.targetTask, programs, role).isEmpty)
    missingRole match
      case Some(_) => IO.pure(testcaseCompileError(testcase))
      case None =>
        subtask.mode.interactor.flatMap(interactor => tools.interactors.get(interactor.source.path).map(interactor -> _)) match
          case None => IO.pure(testcaseSystemError(testcase, JudgeFailureReason.InteractorCompileFailed))
          case Some((interactor, interactorCommand)) =>
            prepareHackStrategyProvider(hackTask, testcase, config, workingDirectory).flatMap {
              case Left(_) => IO.pure(testcaseAcceptedByProtocol(testcase))
              case Right(strategyProvider) =>
                val roleCommands = subtask.mode.roles.flatMap(role => SubmissionJudgeRunner.programCommand(hackTask.targetTask, programs, role).map(role -> _)).toMap
                InteractiveTestcaseRunner.runWithStrategy(
                  task = hackTask.targetTask,
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
            }

  /** 将 hack 请求携带的策略 provider 源码编译为本次交互运行的可选辅助工具。 */
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
            JudgeToolPreparation.compileCppToolBytes(
              task = hackTask.targetTask,
              config = config,
              workingDirectory = workingDirectory,
              sourceNameHint = s"hack-strategy-${hackTask.hackId}",
              sourceBytes = source.getBytes(StandardCharsets.UTF_8)
            ).map {
              case JudgeToolPreparation.ToolCompileOutcome.Success(command) => Right(Some(StrategyProviderRuntime(provider, command)))
              case _ => Left(())
            }

  /** 从原子任务中复制一个非 hack 测试点作为 hack 运行模板，并分配新的测试点索引。 */
  private def templateHackTestcase(subtask: JudgeTaskSubtask): Option[JudgeTaskTestcase] =
    // FIXME-CN: 模板选择第一个非 Hack 测试点，若子任务内不同测试点资源/checker/策略不同，hack 运行语义可能与被攻击点不一致。
    subtask.testcases.find(_.testcaseType != JudgeTestcaseType.Hack).orElse(subtask.testcases.headOption).map { template =>
      val nextIndex = subtask.testcases.map(_.index).maxOption.getOrElse(0) + 1
      template.copy(index = nextIndex, label = Some("hack"), testcaseType = JudgeTestcaseType.Hack, scoreRatio = BigDecimal(1), answer = None)
    }

  /** 构造 failed 状态的 hack 回报，保留旧分数并填充目标侧错误消息。 */
  private def hackFailed(
    task: HackTask,
    targetMessage: String,
    standardMessage: Option[String] = None
  ): ReportHackResultRequest =
    ReportHackResultRequest(
      status = "failed",
      answer = None,
      oldScore = targetSubtaskWorstScore(task),
      newScore = None,
      newResult = None,
      validatorMessage = None,
      standardMessage = standardMessage,
      targetMessage = Some(targetMessage)
    )

  /** 从旧结果中读取目标子任务最差分，缺失时按 0 处理。 */
  private def targetSubtaskWorstScore(task: HackTask): BigDecimal =
    // FIXME-CN: 旧结果缺少目标子任务时默认 oldScore=0，会把结果树不完整误判为 hack 无收益。
    task.oldResult.subtasks.find(_.index == task.subtaskIndex).map(_.worstResult.score).getOrElse(BigDecimal(0))

  /** 把新增 hack 测试点追加到旧子任务结果后重新聚合。 */
  private def appendHackTestcaseResult(
    subtask: JudgeTaskSubtask,
    oldSubtasks: List[JudgeSubtaskResult],
    testcaseResult: JudgeTestcaseResult
  ): JudgeSubtaskResult =
    val existingTestcases = oldSubtasks.find(_.index == subtask.index).map(_.testcases).getOrElse(Nil)
    JudgeResultAggregator.aggregateSubtask(subtask, existingTestcases :+ testcaseResult)

  /** 用新子任务结果替换旧结果树中的同 index 子任务，缺失时追加。 */
  private def replaceSubtaskResult(subtasks: List[JudgeSubtaskResult], result: JudgeSubtaskResult): List[JudgeSubtaskResult] =
    if subtasks.exists(_.index == result.index) then
      subtasks.map(current => if current.index == result.index then result else current)
    else
      subtasks :+ result
