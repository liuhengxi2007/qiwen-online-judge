package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{SubmissionLanguage, SubmissionStatus}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult, JudgeSubtaskResult, JudgeTask, JudgeTaskSubtask, JudgeTaskTestcase}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*
import judger.infra.JudgeTestcaseResults.*
import judger.objects.RuntimeCommand

import java.nio.file.Path

/** 普通提交判题主流程，负责准备程序/工具、执行各子任务并汇总成回报请求。 */
object SubmissionJudgeRunner:
  /** 执行完整提交判题；副作用包括创建工作目录、初始化 isolate、下载题目数据和运行程序。 */
  def judge(
    task: JudgeTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportJudgeResultRequest] =
    withWorkingDirectory(config.workRoot, "qiwen-judger-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        JudgeToolPreparation.preparePrograms(task, config, workingDirectory, problemDataCache, runtimes).flatMap {
          case Left(result) => IO.pure(result)
          case Right(programs) =>
            JudgeToolPreparation.prepareTools(task, config, workingDirectory, problemDataCache).flatMap {
              case Left(result) => IO.pure(result)
              case Right(tools) => judgeSubtasks(task, config, workingDirectory, sandbox, problemDataCache, programs, tools)
            }
        }
      }
    }.handleError { _ =>
      taskSystemError(task, JudgeFailureReason.JudgerRuntimeFailed)
    }

  /** 顺序执行所有子任务并聚合整题结果。 */
  private def judgeSubtasks(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path,
    sandbox: SandboxSession,
    problemDataCache: ProblemDataCache,
    programs: JudgeToolPreparation.PreparedPrograms,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[ReportJudgeResultRequest] =
    task.subtasks.traverse(subtask => judgeSubtask(task, config, subtask, workingDirectory, sandbox, problemDataCache, programs, tools)).map { subtasks =>
      val result = JudgeResultAggregator.aggregateTask(task, subtasks)
      ReportJudgeResultRequest(
        status = if JudgeResultAggregator.containsSystemError(result) then SubmissionStatus.Failed else SubmissionStatus.Completed,
        judgeResult = Some(result)
      )
    }

  /** 根据子任务模式分派传统题或交互题执行；未知模式返回系统错误。 */
  private def judgeSubtask(
    task: JudgeTask,
    config: AppConfig,
    subtask: JudgeTaskSubtask,
    workingDirectory: Path,
    sandbox: SandboxSession,
    problemDataCache: ProblemDataCache,
    programs: JudgeToolPreparation.PreparedPrograms,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeSubtaskResult] =
    subtask.mode.`type` match
      case "traditional" =>
        subtask.testcases.traverse { testcase =>
          val selection = TraditionalProgramSelector.select(task, subtask, testcase, programs)
          selection match
            case TraditionalProgramSelector.TraditionalProgramSelection.CompileError =>
              IO.pure(testcaseCompileError(testcase))
            case selected =>
              loadTestcaseData(task, testcase, problemDataCache).flatMap {
                case Left(reason) => IO.pure(testcaseSystemError(testcase, reason))
                case Right((input, answerBytes)) =>
                  TraditionalTestcaseRunner.runData(task, subtask.index, testcase, workingDirectory, sandbox, input, answerBytes, selected, tools)
              }
        }.map(testcases => JudgeResultAggregator.aggregateSubtask(subtask, testcases))
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
                  loadTestcaseData(task, testcase, problemDataCache).flatMap {
                    case Left(reason) => IO.pure(testcaseSystemError(testcase, reason))
                    case Right((input, answerBytes)) =>
                      InteractiveTestcaseRunner.run(
                        task,
                        config,
                        subtask,
                        testcase,
                        workingDirectory,
                        sandbox,
                        input,
                        answerBytes,
                        roleCommands,
                        interactor,
                        interactorCommand,
                        tools
                      )
                  }
                }.map(testcases => JudgeResultAggregator.aggregateSubtask(subtask, testcases))
      case _ =>
        // FIXME-CN: 未知 mode.type 只折叠为 SystemError，backend/judge.yaml 协议扩展时缺少显式兼容性错误。
        IO.pure(subtaskSystemError(subtask, JudgeFailureReason.SystemError))

  /** 查询某个 role 的可执行命令；不存在或编译失败时返回 None。 */
  private[infra] def programCommand(task: JudgeTask, programs: JudgeToolPreparation.PreparedPrograms, role: String): Option[RuntimeCommand] =
    if !task.programs.contains(role) || programs.compileFailedRoles.contains(role) then None
    else programs.commands.get(role)

  /** 下载单个测试点输入和可选答案；下载或 hash 校验失败统一映射为 ProblemDataLoadFailed。 */
  private[infra] def loadTestcaseData(
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
