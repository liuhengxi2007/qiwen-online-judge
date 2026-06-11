package judger.infra

import cats.effect.IO
import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.{JudgeTask, JudgeTaskTestcase, JudgeTestcaseResult}
import judger.infra.JudgeTestcaseResults.*
import judger.infra.TraditionalProgramSelector.TraditionalProgramSelection
import judger.objects.{ProcessResult, RuntimeCommand, SandboxLimits, SandboxRunSpec, SandboxStdin, SandboxStdout}

import java.nio.file.Path

object TraditionalTestcaseRunner:
  private[infra] def runData(
    task: JudgeTask,
    subtaskIndex: Int,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    selection: TraditionalProgramSelection,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeTestcaseResult] =
    selection match
      case TraditionalProgramSelection.CompileError =>
        IO.pure(testcaseCompileError(testcase))
      case TraditionalProgramSelection.TextOutput(output) =>
        scoreParticipantRun(task, testcase, workingDirectory, sandbox, input, successfulTextRun(output), answerBytes, tools)
      case TraditionalProgramSelection.Command(command) =>
        runCommand(task, subtaskIndex, testcase, workingDirectory, sandbox, input, answerBytes, command, tools)

  private[infra] def scoreParticipantRun(
    task: JudgeTask,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    runResult: ProcessResult,
    answerBytes: Option[Array[Byte]],
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeTestcaseResult] =
    if runResult.timedOut then
      IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.TimeLimitExceeded, None, None, runResult))
    else if runResult.exitCode.getOrElse(-1) != 0 then
      IO.pure(testcaseResult(testcase, BigDecimal(0), SubmissionVerdict.RuntimeError, None, None, runResult))
    else
      CheckerRunner.score(task, testcase, workingDirectory, sandbox, input, runResult.stdout, answerBytes, tools.checkers).map {
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

  private def runCommand(
    task: JudgeTask,
    subtaskIndex: Int,
    testcase: JudgeTaskTestcase,
    workingDirectory: Path,
    sandbox: SandboxSession,
    input: Array[Byte],
    answerBytes: Option[Array[Byte]],
    command: RuntimeCommand,
    tools: JudgeToolPreparation.PreparedTools
  ): IO[JudgeTestcaseResult] =
    sandbox
      .run(
        SandboxRunSpec(
          phase = s"run-$subtaskIndex-${testcase.index}",
          command = command,
          stdin = SandboxStdin.Bytes(input),
          stdout = SandboxStdout.Capture,
          limits = SandboxLimits.runtime(testcase.limits.timeMs.value.toLong, testcase.limits.memoryMb.value)
        ),
        workingDirectory
      )
      .flatMap(runResult => scoreParticipantRun(task, testcase, workingDirectory, sandbox, input, runResult, answerBytes, tools))
