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

object SubmissionJudgeRunner:
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
        IO.pure(subtaskSystemError(subtask, JudgeFailureReason.SystemError))

  private[infra] def programCommand(task: JudgeTask, programs: JudgeToolPreparation.PreparedPrograms, role: String): Option[RuntimeCommand] =
    if !task.programs.contains(role) || programs.compileFailedRoles.contains(role) then None
    else programs.commands.get(role)

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
