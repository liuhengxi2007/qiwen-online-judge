package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionStatus, SubmissionVerdict}
import judger.config.AppConfig

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Base64
import java.util.concurrent.TimeUnit

final case class ProcessResult(
  exitCode: Option[Int],
  stdout: String,
  stderr: String,
  timedOut: Boolean
)

object Cpp17JudgeExecutor:
  def judge(task: JudgeTask, config: AppConfig): IO[ReportJudgeResultRequest] =
    withWorkingDirectory("qiwen-judger-") { workingDirectory =>
      val sourceFile = workingDirectory.resolve("main.cpp")
      val executableName = if isWindows then "main.exe" else "main"
      val executablePath = workingDirectory.resolve(executableName)

      for
        _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode.value, StandardCharsets.UTF_8))
        compileResult <- runProcess(
          command = config.cxx,
          args = List("main.cpp", "-o", "main", "-O2", "-std=c++17"),
          cwd = workingDirectory,
          stdin = None,
          timeoutMs = math.max(task.timeLimitMs.value.toLong * 5, 10000L)
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
            judgeTestcases(task, workingDirectory, executablePath, config)
      yield result
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
    executablePath: Path,
    config: AppConfig
  ): IO[ReportJudgeResultRequest] =
    task.testcases.foldLeft(IO.pure(Option.empty[ReportJudgeResultRequest])) { (accIo, testcase) =>
      accIo.flatMap {
        case some @ Some(_) => IO.pure(some)
        case None =>
          val input = Base64.getDecoder.decode(testcase.inputBase64)
          val expectedOutput = new String(Base64.getDecoder.decode(testcase.expectedOutputBase64), StandardCharsets.UTF_8)
          runProcess(
            command = executablePath.toAbsolutePath.toString,
            args = Nil,
            cwd = workingDirectory,
            stdin = Some(input),
            timeoutMs = math.max(task.timeLimitMs.value.toLong, 1L)
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
          judgeMessage = Some(s"Accepted by ${config.judgerName.value}.")
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

  private def runProcess(
    command: String,
    args: List[String],
    cwd: Path,
    stdin: Option[Array[Byte]],
    timeoutMs: Long
  ): IO[ProcessResult] =
    IO.blocking {
      val builder = new ProcessBuilder((command :: args)*)
      builder.directory(cwd.toFile)
      val process = builder.start()

      stdin.foreach { bytes =>
        val stream = process.getOutputStream
        try stream.write(bytes)
        finally stream.close()
      }
      if stdin.isEmpty then process.getOutputStream.close()

      val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      val stdout = new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
      val stderr = new String(process.getErrorStream.readAllBytes(), StandardCharsets.UTF_8)

      ProcessResult(
        exitCode = if completed then Some(process.exitValue()) else None,
        stdout = stdout,
        stderr = stderr,
        timedOut = !completed
      )
    }

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback

  private def isWindows: Boolean =
    sys.props.get("os.name").exists(_.toLowerCase.contains("win"))
