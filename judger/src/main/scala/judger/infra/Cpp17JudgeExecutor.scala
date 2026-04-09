package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionStatus, SubmissionVerdict}
import judger.config.AppConfig

import java.io.ByteArrayOutputStream
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

private final case class Sandbox(
  boxRoot: Path,
  boxId: Int,
  useCgroups: Boolean
)

object Cpp17JudgeExecutor:
  def judge(task: JudgeTask, config: AppConfig): IO[ReportJudgeResultRequest] =
    withWorkingDirectory("qiwen-judger-") { workingDirectory =>
      withSandbox(config) { sandbox =>
        val sourceFile = sandbox.boxRoot.resolve("main.cpp")
        val executablePath = sandbox.boxRoot.resolve("main")
        val compilerPath = resolveExecutable(config.cxx).getOrElse(config.cxx)

        for
          _ <- IO.blocking(Files.writeString(sourceFile, task.sourceCode.value, StandardCharsets.UTF_8))
          compileResult <- runIsolatedProcess(
            phase = "compile",
            command = compilerPath,
            args = List("main.cpp", "-o", "main", "-O2", "-std=c++17"),
            stdin = None,
            timeoutMs = 10000L,
            memoryLimitMb = 1024,
            allowChildProcesses = true,
            hostWorkingDirectory = workingDirectory,
            sandbox = sandbox,
            config = config
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
              judgeTestcases(task, workingDirectory, executablePath, sandbox, config)
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
    executablePath: Path,
    sandbox: Sandbox,
    config: AppConfig
  ): IO[ReportJudgeResultRequest] =
    task.testcases.foldLeft(IO.pure(Option.empty[ReportJudgeResultRequest])) { (accIo, testcase) =>
      accIo.flatMap {
        case some @ Some(_) => IO.pure(some)
        case None =>
          val input = Base64.getDecoder.decode(testcase.inputBase64)
          val expectedOutput = new String(Base64.getDecoder.decode(testcase.expectedOutputBase64), StandardCharsets.UTF_8)
          runIsolatedProcess(
            phase = s"run-${testcase.name.value}",
            command = "./main",
            args = Nil,
            stdin = Some(input),
            timeoutMs = math.max(task.timeLimitMs.value.toLong, 1L),
            memoryLimitMb = task.spaceLimitMb.value,
            allowChildProcesses = false,
            hostWorkingDirectory = workingDirectory,
            sandbox = sandbox,
            config = config
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

  private def withSandbox[A](config: AppConfig)(use: Sandbox => IO[A]): IO[A] =
    initializeSandbox(config).bracket(use) { sandbox =>
      cleanupSandbox(config, sandbox)
    }

  private def initializeSandbox(config: AppConfig): IO[Sandbox] =
    IO.blocking {
      if config.preferIsolateCgroups then
        initializeSandboxAttempt(config, useCgroups = true).getOrElse(initializeSandboxUnsafe(config, useCgroups = false))
      else initializeSandboxUnsafe(config, useCgroups = false)
    }

  private def cleanupSandbox(config: AppConfig, sandbox: Sandbox): IO[Unit] =
    IO.blocking {
      val process = new ProcessBuilder(
        (List(config.isolateBin, s"--box-id=${sandbox.boxId}") ++
          (if sandbox.useCgroups then List("--cg") else Nil) ++
          List("--cleanup"))*
      ).start()
      process.waitFor(10, TimeUnit.SECONDS)
      ()
    }.void.handleError(_ => ())

  private def runIsolatedProcess(
    phase: String,
    command: String,
    args: List[String],
    stdin: Option[Array[Byte]],
    timeoutMs: Long,
    memoryLimitMb: Int,
    allowChildProcesses: Boolean,
    hostWorkingDirectory: Path,
    sandbox: Sandbox,
    config: AppConfig
  ): IO[ProcessResult] =
    IO.blocking {
      val metaPath = hostWorkingDirectory.resolve(s"${sanitizeFilename(phase)}.meta")
      val stdoutPath = hostWorkingDirectory.resolve(s"${sanitizeFilename(phase)}.stdout")
      val stderrPath = hostWorkingDirectory.resolve(s"${sanitizeFilename(phase)}.stderr")
      Files.deleteIfExists(metaPath)
      Files.deleteIfExists(stdoutPath)
      Files.deleteIfExists(stderrPath)

      val isolateArgs =
        List(
          config.isolateBin,
          s"--box-id=${sandbox.boxId}",
          s"--meta=${metaPath.toAbsolutePath}",
          s"--stdout=${stdoutPath.toAbsolutePath}",
          s"--stderr=${stderrPath.toAbsolutePath}",
          s"--time=${secondsCeil(timeoutMs)}",
          s"--wall-time=${secondsCeil(wallTimeMs(timeoutMs))}",
          s"--mem=${math.max(memoryLimitMb, 16) * 1024}"
        ) ++
          (if sandbox.useCgroups then List("--cg") else Nil) ++
          (if allowChildProcesses then List("--processes=64") else Nil) ++
          List("--run", "--", command) ++ args

      val builder = new ProcessBuilder(isolateArgs*)
      builder.directory(sandbox.boxRoot.toFile)
      val process = builder.start()

      stdin.foreach { bytes =>
        val stream = process.getOutputStream
        try stream.write(bytes)
        finally stream.close()
      }
      if stdin.isEmpty then process.getOutputStream.close()

      val completed = process.waitFor(timeoutMs + 5000L, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      val launcherStdout = readStream(process.getInputStream)
      val launcherStderr = readStream(process.getErrorStream)
      val meta = readMeta(metaPath)
      val stdout = readOptionalFile(stdoutPath)
      val stderr = nonEmptyOrFallback(readOptionalFile(stderrPath), launcherStderr, launcherStdout)

      ProcessResult(
        exitCode = meta
          .get("exitcode")
          .flatMap(value => value.toIntOption)
          .orElse(if completed then Some(process.exitValue()) else None),
        stdout = stdout,
        stderr = stderr,
        timedOut = !completed || meta.get("status").contains("TO")
      )
    }

  private def readOptionalFile(path: Path): String =
    if Files.exists(path) then Files.readString(path, StandardCharsets.UTF_8) else ""

  private def readMeta(path: Path): Map[String, String] =
    if !Files.exists(path) then Map.empty
    else
      Files
        .readAllLines(path, StandardCharsets.UTF_8)
        .toArray(Array[String]())
        .iterator
        .flatMap { line =>
          line.split(":", 2) match
            case Array(key, value) => Some(key.trim -> value.trim)
            case _ => None
        }
        .toMap

  private def readStream(stream: java.io.InputStream): String =
    val buffer = ByteArrayOutputStream()
    stream.transferTo(buffer)
    buffer.toString(StandardCharsets.UTF_8)

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

  private def sanitizeFilename(value: String): String =
    value.map {
      case current if current.isLetterOrDigit => current
      case _ => '_'
    }

  private def secondsCeil(milliseconds: Long): Long =
    math.max(1L, (milliseconds + 999L) / 1000L)

  private def wallTimeMs(timeLimitMs: Long): Long =
    math.max(1L, (timeLimitMs * 3 + 1) / 2 + 500L)

  private def initializeSandboxAttempt(config: AppConfig, useCgroups: Boolean): Option[Sandbox] =
    scala.util.Try(initializeSandboxUnsafe(config, useCgroups)).toOption

  private def initializeSandboxUnsafe(config: AppConfig, useCgroups: Boolean): Sandbox =
    val process = new ProcessBuilder(
      (List(config.isolateBin, s"--box-id=${config.isolateBoxId}") ++
        (if useCgroups then List("--cg") else Nil) ++
        List("--init"))*
    ).start()
    val stdout = readStream(process.getInputStream)
    val stderr = readStream(process.getErrorStream)
    val exitCode = process.waitFor()
    if exitCode != 0 then
      throw RuntimeException(nonEmptyOrFallback(stderr, stdout, s"Failed to initialize isolate sandbox (exit=$exitCode)."))

    val boxRoot = stdout.linesIterator.map(_.trim).find(_.nonEmpty).getOrElse {
      throw IllegalStateException("isolate --init returned no sandbox path.")
    }
    Sandbox(boxRoot = Path.of(boxRoot), boxId = config.isolateBoxId, useCgroups = useCgroups)

  private def normalizeOutput(value: String): String =
    value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing()

  private def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback
