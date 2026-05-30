package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.JudgeTask
import judger.config.AppConfig
import judger.objects.{ProcessResult, RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

trait JudgeRuntime:
  def language: SubmissionLanguage

  def prepare(
    task: JudgeTask,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ReportJudgeResultRequest, RuntimeCommand]]

object JudgeRuntimeSupport:
  def withWorkingDirectory[A](workRoot: Path, prefix: String)(use: Path => IO[A]): IO[A] =
    IO.blocking {
      Files.createDirectories(workRoot)
      val path = Files.createTempDirectory(workRoot, prefix)
      ensureSandboxAccessible(path)
      path
    }.bracket(use)(deleteRecursively)

  def resolveExecutable(command: String): Option[String] =
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

  def isSandboxVisibleExecutable(path: String): Boolean =
    path.startsWith("/usr/") || path == "/usr" || path.startsWith("/bin/") || path == "/bin"

  def runHostProcess(
    command: String,
    args: List[String],
    cwd: Path,
    stdin: Option[Array[Byte]],
    limits: SandboxLimits,
    stdoutName: String,
    stderrName: String
  ): IO[ProcessResult] =
    IO.blocking {
      val stdoutPath = cwd.resolve(stdoutName)
      val stderrPath = cwd.resolve(stderrName)
      Files.deleteIfExists(stdoutPath)
      Files.deleteIfExists(stderrPath)
      val builder = new ProcessBuilder(
        (
          List(
            "prlimit",
            s"--as=${limits.memoryLimitKb.value * 1024L}",
            s"--cpu=${math.max(1L, limits.timeLimit.value / 1000L)}",
            "--"
          ) ++ (command :: args)
        )*
      )
      builder.directory(cwd.toFile)
      builder.redirectOutput(stdoutPath.toFile)
      builder.redirectError(stderrPath.toFile)
      val process = builder.start()

      stdin.foreach { bytes =>
        val stream = process.getOutputStream
        try stream.write(bytes)
        finally stream.close()
      }
      if stdin.isEmpty then process.getOutputStream.close()

      val completed = process.waitFor(limits.wallTimeLimit.value, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      ProcessResult(
        exitCode = if completed then Some(process.exitValue()) else None,
        isolateStatus = None,
        isolateMessage = None,
        stdout = readOptionalFile(stdoutPath),
        stderr = readOptionalFile(stderrPath),
        timedOut = !completed,
        timeUsedMs = None,
        memoryUsedKb = None
      )
    }

  def ensureExecutableExists(path: Path): IO[Unit] =
    IO.blocking {
      if !Files.exists(path) then
        throw RuntimeException(s"Prepared executable was not produced at ${path.toAbsolutePath}.")
      if !Files.isRegularFile(path) then
        throw RuntimeException(s"Prepared executable path is not a regular file: ${path.toAbsolutePath}.")
      if !Files.isExecutable(path) then
        throw RuntimeException(s"Prepared executable is not executable: ${path.toAbsolutePath}.")
    }

  def completed(verdict: SubmissionVerdict, message: String = ""): ReportJudgeResultRequest =
    ReportJudgeResultRequest(
      status = SubmissionStatus.Completed,
      verdict = Some(verdict),
      judgeMessage = Option.when(message.nonEmpty)(message),
      timeUsedMs = None,
      memoryUsedKb = None,
      score = None,
      judgeResult = None
    )

  def systemError(message: String): ReportJudgeResultRequest =
    ReportJudgeResultRequest(
      status = SubmissionStatus.Failed,
      verdict = Some(SubmissionVerdict.SystemError),
      judgeMessage = Some(message),
      timeUsedMs = None,
      memoryUsedKb = None,
      score = None,
      judgeResult = None
    )

  def renderDetail(detail: String, result: ProcessResult, includeIsolateDetail: Boolean = false): String =
    val isolateDetail =
      if includeIsolateDetail then
        List(
          result.isolateStatus.map(status => s"isolate status: $status"),
          result.isolateMessage.map(message => s"isolate message: $message")
        ).flatten
      else Nil

    List(
      Some(detail),
      Option.when(isolateDetail.nonEmpty)(isolateDetail.mkString("\n")),
      namedSection("stderr", result.stderr),
      namedSection("stdout", result.stdout)
    ).flatten.mkString("\n\n")

  def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback

  private def deleteRecursively(path: Path): IO[Unit] =
    IO.blocking {
      if Files.exists(path) then
        val paths = Files.walk(path)
        try paths.sorted(java.util.Comparator.reverseOrder()).forEach(currentPath => Files.deleteIfExists(currentPath))
        finally paths.close()
    }.void.handleError(_ => ())

  private def ensureSandboxAccessible(path: Path): Unit =
    scala.util.Try {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"))
    }
    ()

  private def resolveRealExecutablePath(path: Path): Option[String] =
    scala.util.Try(path.toRealPath().toString).toOption

  private def readOptionalFile(path: Path): String =
    if Files.exists(path) then Files.readString(path, StandardCharsets.UTF_8) else ""

  private def namedSection(label: String, value: String): Option[String] =
    Option.when(value.trim.nonEmpty)(s"$label:\n${value.trim}")
