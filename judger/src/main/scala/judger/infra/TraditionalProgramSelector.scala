package judger.infra

import judgeprotocol.objects.SubmissionLanguage
import judgeprotocol.objects.response.{JudgeTask, JudgeTaskSubtask, JudgeTaskTestcase}
import judger.objects.RuntimeCommand

object TraditionalProgramSelector:
  private[judger] enum TraditionalProgramSelection:
    case Command(command: RuntimeCommand)
    case TextOutput(output: String)
    case CompileError

  private[judger] def select(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    programs: JudgeToolPreparation.PreparedPrograms
  ): TraditionalProgramSelection =
    val roles = if testcase.roles.nonEmpty then testcase.roles else List(subtask.mode.role.getOrElse("main"))
    roles.collectFirst {
      case role if task.programs.contains(role) =>
        task.programs(role).language match
          case SubmissionLanguage.Text =>
            programs.textOutputs.get(role) match
              case Some(output) => TraditionalProgramSelection.TextOutput(output)
              case None => TraditionalProgramSelection.CompileError
          case _ if programs.compileFailedRoles.contains(role) =>
            TraditionalProgramSelection.CompileError
          case _ =>
            programs.commands.get(role) match
              case Some(command) => TraditionalProgramSelection.Command(command)
              case None => TraditionalProgramSelection.CompileError
    }.getOrElse(TraditionalProgramSelection.CompileError)
