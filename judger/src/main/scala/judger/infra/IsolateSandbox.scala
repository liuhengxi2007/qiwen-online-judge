package judger.infra

import cats.effect.IO
import judger.config.AppConfig
import judger.objects.{ProcessResult, SandboxExecutionRequest}

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

private final case class SandboxState(
  boxId: Int,
  useCgroups: Boolean
)

final class IsolateSandbox private (private val state: SandboxState, config: AppConfig):
  def run(request: SandboxExecutionRequest, hostWorkingDirectory: Path): IO[ProcessResult] =
    IO.blocking {
      val safePhase = IsolateSandbox.sanitizeFilename(request.phase)
      val metaPath = hostWorkingDirectory.resolve(s"$safePhase.meta")
      val defaultStdinPath = hostWorkingDirectory.resolve(s"$safePhase.stdin")
      val stdoutFile = request.stdoutFile.getOrElse(s"$safePhase.stdout")
      val stdoutPath = hostWorkingDirectory.resolve(stdoutFile)
      val stderrPath = hostWorkingDirectory.resolve(s"$safePhase.stderr")
      Files.deleteIfExists(metaPath)
      if request.stdinFile.isEmpty then Files.deleteIfExists(defaultStdinPath)
      if request.stdoutFile.isEmpty then Files.deleteIfExists(stdoutPath)
      Files.deleteIfExists(stderrPath)
      request.stdin.foreach(bytes => Files.write(defaultStdinPath, bytes))
      val stdinArgs =
        request.stdinFile
          .orElse(Option.when(request.stdin.nonEmpty)(defaultStdinPath.getFileName.toString))
          .map(path => List(s"--stdin=$path"))
          .getOrElse(Nil)

      val isolateArgs =
        List(
          config.isolateBin,
          s"--box-id=${state.boxId}",
          s"--dir=/box=${hostWorkingDirectory.toAbsolutePath}:rw",
          s"--meta=${metaPath.toAbsolutePath}",
          s"--stdout=$stdoutFile",
          s"--stderr=${stderrPath.getFileName}",
          "--chdir=/box",
          s"--time=${IsolateSandbox.secondsCeil(request.limits.timeLimit.value)}",
          s"--wall-time=${IsolateSandbox.secondsCeil(request.limits.wallTimeLimit.value)}",
          s"--mem=${request.limits.memoryLimitKb.value}"
        ) ++ stdinArgs ++
          (
          (if state.useCgroups then List("--cg") else Nil) ++
          List(s"--processes=${math.max(request.processLimit, 1)}") ++
          List("--run", "--", request.command) ++ request.args
          )

      val builder = new ProcessBuilder(isolateArgs*)
      builder.directory(hostWorkingDirectory.toFile)
      val process = builder.start()
      process.getOutputStream.close()

      val completed = process.waitFor(request.limits.wallTimeLimit.value + 5000L, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      val launcherStdout = IsolateSandbox.readStream(process.getInputStream)
      val launcherStderr = IsolateSandbox.readStream(process.getErrorStream)
      val meta = IsolateSandbox.readMeta(metaPath)
      val stdout = if request.captureStdout then IsolateSandbox.readOptionalFile(stdoutPath) else ""
      val stderr = IsolateSandbox.nonEmptyOrFallback(IsolateSandbox.readOptionalFile(stderrPath), launcherStderr, launcherStdout)

      ProcessResult(
        exitCode = meta
          .get("exitcode")
          .flatMap(_.toIntOption)
          .orElse(if completed then Some(process.exitValue()) else None),
        isolateStatus = meta.get("status"),
        isolateMessage = meta.get("message"),
        stdout = stdout,
        stderr = stderr,
        timedOut = !completed || meta.get("status").contains("TO"),
        timeUsedMs = IsolateSandbox.timeUsedMs(meta),
        wallTimeUsedMs = IsolateSandbox.wallTimeUsedMs(meta),
        memoryUsedKb = IsolateSandbox.memoryUsedKb(meta)
      )
    }

  private[judger] def runInBox(boxOffset: Int, request: SandboxExecutionRequest, hostWorkingDirectory: Path): IO[ProcessResult] =
    val boxId = IsolateSandbox.boxIdAt(state.boxId, boxOffset)
    IsolateSandbox
      .initialize(config, boxId, state.useCgroups)
      .bracket(tempState => new IsolateSandbox(tempState, config).run(request, hostWorkingDirectory))(tempState =>
        IsolateSandbox.cleanup(config, tempState)
      )

  private[judger] def cleanup(): IO[Unit] =
    IsolateSandbox.cleanup(config, state)

object IsolateSandbox:
  def resource[A](config: AppConfig)(use: IsolateSandbox => IO[A]): IO[A] =
    initialize(config).bracket(use)(_.cleanup())

  private def initialize(config: AppConfig): IO[IsolateSandbox] =
    IO.blocking {
      val state =
        if config.preferIsolateCgroups then
          initializeAttempt(config, config.isolateBoxId, useCgroups = true).getOrElse(initializeUnsafe(config, config.isolateBoxId, useCgroups = false))
        else initializeUnsafe(config, config.isolateBoxId, useCgroups = false)
      new IsolateSandbox(state, config)
    }

  private def initialize(config: AppConfig, boxId: Int, useCgroups: Boolean): IO[SandboxState] =
    IO.blocking(initializeUnsafe(config, boxId, useCgroups))

  private def initializeAttempt(config: AppConfig, boxId: Int, useCgroups: Boolean): Option[SandboxState] =
    scala.util.Try(initializeUnsafe(config, boxId, useCgroups)).toOption

  private def initializeUnsafe(config: AppConfig, boxId: Int, useCgroups: Boolean): SandboxState =
    val process = new ProcessBuilder(
      (List(config.isolateBin, s"--box-id=$boxId") ++
        (if useCgroups then List("--cg") else Nil) ++
        List("--init"))*
    ).start()
    val stdout = readStream(process.getInputStream)
    val stderr = readStream(process.getErrorStream)
    val exitCode = process.waitFor()
    if exitCode != 0 then
      throw RuntimeException(nonEmptyOrFallback(stderr, stdout, s"Failed to initialize isolate sandbox (exit=$exitCode)."))

    stdout.linesIterator.map(_.trim).find(_.nonEmpty).getOrElse {
      throw IllegalStateException("isolate --init returned no sandbox path.")
    }
    SandboxState(boxId = boxId, useCgroups = useCgroups)

  private def cleanup(config: AppConfig, state: SandboxState): IO[Unit] =
    IO.blocking {
      val process = new ProcessBuilder(
        (List(config.isolateBin, s"--box-id=${state.boxId}") ++
          (if state.useCgroups then List("--cg") else Nil) ++
          List("--cleanup"))*
      ).start()
      process.waitFor(10, TimeUnit.SECONDS)
      ()
    }.void.handleError(_ => ())

  private[judger] def readOptionalFile(path: Path): String =
    if Files.exists(path) && Files.isRegularFile(path) then Files.readString(path, StandardCharsets.UTF_8) else ""

  private[judger] def readMeta(path: Path): Map[String, String] =
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

  private[judger] def timeUsedMs(meta: Map[String, String]): Option[Long] =
    meta
      .get("time")
      .orElse(meta.get("time-wall"))
      .flatMap(value => scala.util.Try(BigDecimal(value)).toOption)
      .map(seconds => math.max(0L, (seconds * BigDecimal(1000)).setScale(0, BigDecimal.RoundingMode.HALF_UP).toLong))

  private[judger] def wallTimeUsedMs(meta: Map[String, String]): Option[Long] =
    meta
      .get("time-wall")
      .flatMap(value => scala.util.Try(BigDecimal(value)).toOption)
      .map(seconds => math.max(0L, (seconds * BigDecimal(1000)).setScale(0, BigDecimal.RoundingMode.HALF_UP).toLong))

  private[judger] def memoryUsedKb(meta: Map[String, String]): Option[Long] =
    meta.get("max-rss").flatMap(_.toLongOption).map(value => math.max(0L, value))

  private[judger] def readStream(stream: java.io.InputStream): String =
    val buffer = ByteArrayOutputStream()
    stream.transferTo(buffer)
    buffer.toString(StandardCharsets.UTF_8)

  private[judger] def sanitizeFilename(value: String): String =
    value.map {
      case current if current.isLetterOrDigit => current
      case _ => '_'
    }

  private[judger] def secondsCeil(milliseconds: Long): Long =
    math.max(1L, (milliseconds + 999L) / 1000L)

  private[judger] def boxIdAt(baseBoxId: Int, offset: Int): Int =
    Math.floorMod(baseBoxId + offset, 1000)

  private[judger] def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback
