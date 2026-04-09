package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionStatus, SubmissionVerdict}
import judger.config.AppConfig

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import java.util.concurrent.TimeUnit

object Cpp17JudgeExecutor:
  private val CompileLimits = SandboxLimits.runtime(timeLimitMs = 10000L, memoryLimitMb = 1024)

  def judge(task: JudgeTask, config: AppConfig): IO[ReportJudgeResultRequest] =
    resolveCompilerPath(config).flatMap {
      case Left(message) =>
        IO.pure(
          ReportJudgeResultRequest(
            status = SubmissionStatus.Failed,
            verdict = Some(SubmissionVerdict.SystemError),
            judgeMessage = Some(message)
          )
        )
      case Right(compilerPath) =>
        withWorkingDirectory(config.workRoot, "qiwen-judger-") { workingDirectory =>
          IsolateSandbox.resource(config) { sandbox =>
            val sourceFile = workingDirectory.resolve("main.cpp")

            for
              _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode.value, StandardCharsets.UTF_8))
              compileResult <- runHostProcess(
                command = compilerPath,
                args = List("main.cpp", "-o", "main", "-O2", "-std=c++17"),
                cwd = workingDirectory,
                stdin = None,
                timeoutMs = CompileLimits.wallTimeLimit.value
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
                      judgeMessage = Some(formatCompileError(compilerPath, compileResult))
                    )
                  )
                else
                  ensureExecutableExists(workingDirectory.resolve("main")) *> judgeTestcases(task, workingDirectory, sandbox)
            yield result
          }
        }
    }.handleError { error =>
      val message = Option(error.getMessage).map(_.trim).filter(_.nonEmpty).getOrElse(error.getClass.getName)
      ReportJudgeResultRequest(
        status = SubmissionStatus.Failed,
        verdict = Some(SubmissionVerdict.SystemError),
        judgeMessage = Some(s"${error.getClass.getSimpleName}: $message")
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
              command = "/box/main",
              args = Nil,
              stdin = Some(input),
              limits = SandboxLimits.runtime(task.timeLimitMs.value.toLong, task.spaceLimitMb.value),
              processLimit = 1
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
                  judgeMessage = Some(formatRuntimeError(testcase.name.value, runResult))
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

  private def withWorkingDirectory[A](workRoot: Path, prefix: String)(use: Path => IO[A]): IO[A] =
    IO.blocking {
      Files.createDirectories(workRoot)
      val path = Files.createTempDirectory(workRoot, prefix)
      ensureSandboxAccessible(path)
      path
    }.bracket(use) { path =>
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
    if commandPath.isAbsolute && Files.isExecutable(commandPath) then resolveRealExecutablePath(commandPath)
    else
      sys.env
        .get("PATH")
        .toList
        .flatMap(_.split(java.io.File.pathSeparator).toList)
        .map(pathEntry => Path.of(pathEntry).resolve(command))
        .find(path => Files.isRegularFile(path) && Files.isExecutable(path))
        .flatMap(resolveRealExecutablePath)

  private def resolveCompilerPath(config: AppConfig): IO[Either[String, String]] =
    IO.blocking {
      resolveExecutable(config.cxx) match
        case None =>
          Left(s"Compiler '${config.cxx}' was not found on the judger host.")
        case Some(path) if isSandboxVisibleExecutable(path) =>
          Right(path)
        case Some(path) =>
          Left(
            s"Compiler '$path' is not visible inside isolate. " +
              "Use a compiler under /usr or /bin, or adjust the judger configuration."
          )
    }

  private def isSandboxVisibleExecutable(path: String): Boolean =
    path.startsWith("/usr/") || path == "/usr" || path.startsWith("/bin/") || path == "/bin"

  private def resolveRealExecutablePath(path: Path): Option[String] =
    scala.util.Try(path.toRealPath().toString).toOption

  private def ensureSandboxAccessible(path: Path): Unit =
    scala.util.Try {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"))
    }
    ()

  private def formatCompileError(compilerPath: String, result: ProcessResult): String =
    val exitCode = result.exitCode.getOrElse(-1)
    val detail =
      if exitCode == 127 then
        s"Compiler '$compilerPath' was not found on the judger host (exit status 127)."
      else s"Compilation failed with exit status $exitCode using $compilerPath."

    val isolateDetail =
      List(
        result.isolateStatus.map(status => s"isolate status: $status"),
        result.isolateMessage.map(message => s"isolate message: $message")
      ).flatten

    val stderr = result.stderr.trim
    val stdout = result.stdout.trim
    val sections =
      List(
        Some(detail),
        Option.when(isolateDetail.nonEmpty)(isolateDetail.mkString("\n")),
        Option.when(stderr.nonEmpty)(s"stderr:\n$stderr"),
        Option.when(stdout.nonEmpty)(s"stdout:\n$stdout")
      ).flatten

    sections.mkString("\n\n")

  private def formatRuntimeError(testcaseName: String, result: ProcessResult): String =
    val exitCode = result.exitCode.getOrElse(-1)
    val detail =
      if exitCode == 127 then
        s"Executable './main' was not found or was not executable inside isolate sandbox on testcase $testcaseName."
      else
        s"Runtime error on testcase $testcaseName (exit status $exitCode)."

    val isolateDetail =
      List(
        result.isolateStatus.map(status => s"isolate status: $status"),
        result.isolateMessage.map(message => s"isolate message: $message")
      ).flatten

    val stderr = result.stderr.trim
    val stdout = result.stdout.trim
    List(
      Some(detail),
      Option.when(isolateDetail.nonEmpty)(isolateDetail.mkString("\n")),
      Option.when(stderr.nonEmpty)(s"stderr:\n$stderr"),
      Option.when(stdout.nonEmpty)(s"stdout:\n$stdout")
    ).flatten.mkString("\n\n")

  private def runHostProcess(
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

      ProcessResult(
        exitCode = if completed then Some(process.exitValue()) else None,
        isolateStatus = None,
        isolateMessage = None,
        stdout = readStream(process.getInputStream),
        stderr = readStream(process.getErrorStream),
        timedOut = !completed
      )
    }

  private def ensureExecutableExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Compiled executable was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Compiled executable path is not a regular file: ${path.toAbsolutePath}.")
      if !Files.isExecutable(path) then
        throw RuntimeException(s"Compiled executable is not executable: ${path.toAbsolutePath}.")
    }

  private def readStream(stream: java.io.InputStream): String =
    val buffer = ByteArrayOutputStream()
    stream.transferTo(buffer)
    buffer.toString(StandardCharsets.UTF_8)

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback
