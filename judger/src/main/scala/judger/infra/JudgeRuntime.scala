package judger.infra

import cats.effect.IO
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult, JudgeSubtaskResult, JudgeTask}
import judger.config.AppConfig
import judger.objects.{ProcessResult, RuntimeCommand, SandboxLimits}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

trait JudgeRuntime:
  def language: SubmissionLanguage

  def prepare(
    role: String,
    sourceCode: SubmissionSourceCode,
    config: AppConfig,
    workingDirectory: Path
  ): IO[Either[ProgramPrepareFailure, RuntimeCommand]]

sealed trait ProgramPrepareFailure

object ProgramPrepareFailure:
  case object CompileError extends ProgramPrepareFailure
  final case class SystemError(reason: JudgeFailureReason) extends ProgramPrepareFailure

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
        wallTimeUsedMs = None,
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

  def taskCompleted(task: JudgeTask, verdict: SubmissionVerdict): ReportJudgeResultRequest =
    taskResult(task, SubmissionStatus.Completed, verdict, None)

  def taskSystemError(task: JudgeTask, reason: JudgeFailureReason): ReportJudgeResultRequest =
    taskResult(task, SubmissionStatus.Failed, SubmissionVerdict.SystemError, Some(reason))

  private def taskResult(
    task: JudgeTask,
    status: SubmissionStatus,
    verdict: SubmissionVerdict,
    reason: Option[JudgeFailureReason]
  ): ReportJudgeResultRequest =
    val subtasks = task.subtasks.map { subtask =>
      JudgeSubtaskResult(
        index = subtask.index,
        label = subtask.label,
        score = BigDecimal(0),
        verdict = verdict,
        timeUsedMs = None,
        memoryUsedKb = None,
        reason = reason,
        testcases = Nil
      )
    }
    ReportJudgeResultRequest(
      status = status,
      judgeResult = Some(
        JudgeResult(
          score = BigDecimal(0),
          verdict = verdict,
          reason = reason,
          timeUsedMs = None,
          memoryUsedKb = None,
          subtasks = subtasks
        )
      )
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
