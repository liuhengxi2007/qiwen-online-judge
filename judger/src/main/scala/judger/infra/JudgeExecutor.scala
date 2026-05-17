package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionVerdict}
import judger.config.AppConfig
import judger.infra.JudgeRuntimeSupport.*

import java.nio.charset.StandardCharsets
import java.nio.file.Path

object JudgeExecutor:
  def judge(
    task: JudgeTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtime: JudgeRuntime
  ): IO[ReportJudgeResultRequest] =
    withWorkingDirectory(config.workRoot, "qiwen-judger-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        runtime.prepare(task, config, workingDirectory).flatMap {
          case Left(result) =>
            IO.pure(result)
          case Right(command) =>
            judgeTestcases(task, workingDirectory, sandbox, problemDataCache, command)
        }
      }
    }.handleError { error =>
      val message = Option(error.getMessage).map(_.trim).filter(_.nonEmpty).getOrElse(error.getClass.getName)
      systemError(s"${error.getClass.getSimpleName}: $message")
    }

  private def judgeTestcases(
    task: JudgeTask,
    workingDirectory: Path,
    sandbox: IsolateSandbox,
    problemDataCache: ProblemDataCache,
    command: RuntimeCommand
  ): IO[ReportJudgeResultRequest] =
    task.testcases.foldLeft(IO.pure(TestcaseAccumulator.empty)) { (accIo, testcase) =>
      accIo.flatMap {
        case accumulator if accumulator.result.nonEmpty =>
          IO.pure(accumulator)
        case accumulator =>
          for
            input <- problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, testcase.input)
            expectedOutputBytes <- problemDataCache.loadBytes(task.problemSlug, task.problemDataVersion, testcase.expectedOutput)
            expectedOutput = new String(expectedOutputBytes, StandardCharsets.UTF_8)
            runResult <- sandbox.run(
              SandboxExecutionRequest(
                phase = s"run-${testcase.name.value}",
                command = command.command,
                args = command.args,
                stdin = Some(input),
                limits = SandboxLimits.runtime(task.timeLimitMs.value.toLong, task.spaceLimitMb.value),
                processLimit = command.processLimit
              ),
              workingDirectory
            )
          yield
            val nextAccumulator = accumulator.record(runResult)
            if runResult.timedOut then
              nextAccumulator.finish(completed(SubmissionVerdict.TimeLimitExceeded, s"Time limit exceeded on testcase ${testcase.name.value}."))
            else if runResult.exitCode.getOrElse(-1) != 0 then
              nextAccumulator.finish(completed(SubmissionVerdict.RuntimeError, formatRuntimeError(testcase.name.value, command, runResult)))
            else if normalizeOutput(runResult.stdout) != normalizeOutput(expectedOutput) then
              nextAccumulator.finish(completed(SubmissionVerdict.WrongAnswer, s"Wrong answer on testcase ${testcase.name.value}."))
            else nextAccumulator
      }
    }.map(accumulator => accumulator.result.getOrElse(accumulator.attachUsage(completed(SubmissionVerdict.Accepted))))

  private def formatRuntimeError(testcaseName: String, command: RuntimeCommand, result: ProcessResult): String =
    val exitCode = result.exitCode.getOrElse(-1)
    val detail =
      if exitCode == 127 then
        s"Runtime command '${command.command}' was not found or was not executable inside isolate sandbox on testcase $testcaseName."
      else
        s"Runtime error on testcase $testcaseName (exit status $exitCode)."
    renderDetail(detail, result, includeIsolateDetail = true)

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private final case class TestcaseAccumulator(
    maxTimeUsedMs: Option[Long],
    maxMemoryUsedKb: Option[Long],
    result: Option[ReportJudgeResultRequest]
  ):
    def record(runResult: ProcessResult): TestcaseAccumulator =
      copy(
        maxTimeUsedMs = maxOptional(maxTimeUsedMs, runResult.timeUsedMs),
        maxMemoryUsedKb = maxOptional(maxMemoryUsedKb, runResult.memoryUsedKb)
      )

    def finish(result: ReportJudgeResultRequest): TestcaseAccumulator =
      copy(result = Some(attachUsage(result)))

    def attachUsage(result: ReportJudgeResultRequest): ReportJudgeResultRequest =
      result.copy(timeUsedMs = maxTimeUsedMs, memoryUsedKb = maxMemoryUsedKb)

    private def maxOptional(left: Option[Long], right: Option[Long]): Option[Long] =
      (left, right) match
        case (Some(a), Some(b)) => Some(math.max(a, b))
        case (some @ Some(_), None) => some
        case (None, some @ Some(_)) => some
        case (None, None) => None

  private object TestcaseAccumulator:
    val empty: TestcaseAccumulator = TestcaseAccumulator(None, None, None)
