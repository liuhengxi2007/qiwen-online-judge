package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionStatus, SubmissionVerdict}
import judger.config.AppConfig

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Base64

object Cpp17JudgeExecutor:
  private val CompileLimits = SandboxLimits.runtime(timeLimitMs = 10000L, memoryLimitMb = 1024)

  def judge(task: JudgeTask, config: AppConfig): IO[ReportJudgeResultRequest] =
    withWorkingDirectory("qiwen-judger-") { workingDirectory =>
      IsolateSandbox.resource(config) { sandbox =>
        val sourceFile = sandbox.boxRoot.resolve("main.cpp")

        for
          _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode.value, StandardCharsets.UTF_8))
          compileResult <- sandbox.run(
            SandboxExecutionRequest(
              phase = "compile",
              command = resolveExecutable(config.cxx).getOrElse(config.cxx),
              args = List("main.cpp", "-o", "main", "-O2", "-std=c++17"),
              stdin = None,
              limits = CompileLimits,
              allowChildProcesses = true
            ),
            workingDirectory
          )
          result <-
            if compileResult.timedOut then
              IO.pure(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Failed,
                  verdict = Some(SubmissionVerdict.SystemError),
                  judgeMessage = Some("Compilation timed out on the judger machine.")
                )
              )
            else if compileResult.exitCode.getOrElse(-1) != 0 then
              IO.pure(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.CompileError),
                  judgeMessage = Some(nonEmptyOrFallback(compileResult.stderr, compileResult.stdout, "Compilation failed."))
                )
              )
            else
              judgeTestcases(task, workingDirectory, sandbox)
        yield result
      }
    }.handleError { error =>
      ReportJudgeResultRequest(
        status = SubmissionStatus.Failed,
        verdict = Some(SubmissionVerdict.SystemError),
        judgeMessage = Some(error.getMessage)
      )
    }

  private def judgeTestcases(
    task: JudgeTask,
    workingDirectory: Path,
    sandbox: IsolateSandbox
  ): IO[ReportJudgeResultRequest] =
    task.testcases.foldLeft(IO.pure(Option.empty[ReportJudgeResultRequest])) { (accIo, testcase) =>
      accIo.flatMap {
        case some @ Some(_) => IO.pure(some)
        case None =>
          val input = Base64.getDecoder.decode(testcase.inputBase64)
          val expectedOutput = new String(Base64.getDecoder.decode(testcase.expectedOutputBase64), StandardCharsets.UTF_8)

          sandbox.run(
            SandboxExecutionRequest(
              phase = s"run-${testcase.name.value}",
              command = "./main",
              args = Nil,
              stdin = Some(input),
              limits = SandboxLimits.runtime(task.timeLimitMs.value.toLong, task.spaceLimitMb.value),
              allowChildProcesses = false
            ),
            workingDirectory
          ).map { runResult =>
            if runResult.timedOut then
              Some(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.TimeLimitExceeded),
                  judgeMessage = Some(s"Time limit exceeded on testcase ${testcase.name.value}.")
                )
              )
            else if runResult.exitCode.getOrElse(-1) != 0 then
              Some(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.RuntimeError),
                  judgeMessage = Some(nonEmptyOrFallback(runResult.stderr, "", s"Runtime error on testcase ${testcase.name.value}."))
                )
              )
            else if normalizeOutput(runResult.stdout) != normalizeOutput(expectedOutput) then
              Some(
                ReportJudgeResultRequest(
                  status = SubmissionStatus.Completed,
                  verdict = Some(SubmissionVerdict.WrongAnswer),
                  judgeMessage = Some(s"Wrong answer on testcase ${testcase.name.value}.")
                )
              )
            else None
          }
      }
    }.map(
      _.getOrElse(
        ReportJudgeResultRequest(
          status = SubmissionStatus.Completed,
          verdict = Some(SubmissionVerdict.Accepted),
          judgeMessage = None
        )
      )
    )

  private def withWorkingDirectory[A](prefix: String)(use: Path => IO[A]): IO[A] =
    IO.blocking(Files.createTempDirectory(prefix)).bracket(use) { path =>
      deleteRecursively(path)
    }

  private def deleteRecursively(path: Path): IO[Unit] =
    IO.blocking {
      if Files.exists(path) then
        val paths = Files.walk(path)
        try paths.sorted(java.util.Comparator.reverseOrder()).forEach(currentPath => Files.deleteIfExists(currentPath))
        finally paths.close()
    }.void.handleError(_ => ())

  private def resolveExecutable(command: String): Option[String] =
    val commandPath = Path.of(command)
    if commandPath.isAbsolute && Files.isExecutable(commandPath) then Some(commandPath.toString)
    else
      sys.env
        .get("PATH")
        .toList
        .flatMap(_.split(java.io.File.pathSeparator).toList)
        .map(pathEntry => Path.of(pathEntry).resolve(command))
        .find(path => Files.isRegularFile(path) && Files.isExecutable(path))
        .map(_.toString)

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback
