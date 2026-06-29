package judger.infra

import judgeprotocol.objects.SubmissionLanguage
import judgeprotocol.objects.response.{JudgeTask, JudgeTaskSubtask, JudgeTaskTestcase}
import judger.objects.RuntimeCommand

/** 为传统题测试点选择要运行的提交 role，支持 testcase.roles 覆盖子任务默认 role。 */
object TraditionalProgramSelector:
  /** 传统题 role 选择结果：可执行命令、静态输出或编译错误。 */
  private[judger] enum TraditionalProgramSelection:
    case Command(command: RuntimeCommand)
    case TextOutput(output: String)
    case CompileError

  /** 根据任务、子任务和测试点配置选择第一个可用 role；找不到则视为编译错误。 */
  private[judger] def select(
    task: JudgeTask,
    subtask: JudgeTaskSubtask,
    testcase: JudgeTaskTestcase,
    programs: JudgeToolPreparation.PreparedPrograms
  ): TraditionalProgramSelection =
    // 注意：传统题旧 payload 可能没有显式 role，协议默认使用 main。
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
