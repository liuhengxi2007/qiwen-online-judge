package judger.infra

import cats.effect.IO
import judger.config.AppConfig

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

final case class ProcessResult(
  exitCode: Option[Int],
  stdout: String,
  stderr: String,
  timedOut: Boolean
)

final case class SandboxExecutionRequest(
  phase: String,
  command: String,
  args: List[String],
  stdin: Option[Array[Byte]],
  limits: SandboxLimits,
  allowChildProcesses: Boolean
)

private final case class SandboxState(
  boxRoot: Path,
  boxId: Int,
  useCgroups: Boolean
)

final class IsolateSandbox private (private val state: SandboxState, config: AppConfig):
  def boxRoot: Path = state.boxRoot

  def run(request: SandboxExecutionRequest, hostWorkingDirectory: Path): IO[ProcessResult] =
    IO.blocking {
      val safePhase = IsolateSandbox.sanitizeFilename(request.phase)
      val metaPath = hostWorkingDirectory.resolve(s"$safePhase.meta")
      val stdoutPath = hostWorkingDirectory.resolve(s"$safePhase.stdout")
      val stderrPath = hostWorkingDirectory.resolve(s"$safePhase.stderr")
      Files.deleteIfExists(metaPath)
      Files.deleteIfExists(stdoutPath)
      Files.deleteIfExists(stderrPath)

      val isolateArgs =
        List(
          config.isolateBin,
          s"--box-id=${state.boxId}",
          s"--meta=${metaPath.toAbsolutePath}",
          s"--stdout=${stdoutPath.toAbsolutePath}",
          s"--stderr=${stderrPath.toAbsolutePath}",
          s"--time=${IsolateSandbox.secondsCeil(request.limits.timeLimit.value)}",
          s"--wall-time=${IsolateSandbox.secondsCeil(request.limits.wallTimeLimit.value)}",
          s"--mem=${request.limits.memoryLimitKb.value}"
        ) ++
          (if state.useCgroups then List("--cg") else Nil) ++
          (if request.allowChildProcesses then List("--processes=64") else Nil) ++
          List("--run", "--", request.command) ++ request.args

      val builder = new ProcessBuilder(isolateArgs*)
      builder.directory(state.boxRoot.toFile)
      val process = builder.start()

      request.stdin.foreach { bytes =>
        val stream = process.getOutputStream
        try stream.write(bytes)
        finally stream.close()
      }
      if request.stdin.isEmpty then process.getOutputStream.close()

      val completed = process.waitFor(request.limits.wallTimeLimit.value + 5000L, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      val launcherStdout = IsolateSandbox.readStream(process.getInputStream)
      val launcherStderr = IsolateSandbox.readStream(process.getErrorStream)
      val meta = IsolateSandbox.readMeta(metaPath)
      val stdout = IsolateSandbox.readOptionalFile(stdoutPath)
      val stderr = IsolateSandbox.nonEmptyOrFallback(IsolateSandbox.readOptionalFile(stderrPath), launcherStderr, launcherStdout)

      ProcessResult(
        exitCode = meta
          .get("exitcode")
          .flatMap(_.toIntOption)
          .orElse(if completed then Some(process.exitValue()) else None),
        stdout = stdout,
        stderr = stderr,
        timedOut = !completed || meta.get("status").contains("TO")
      )
    }

  private[judger] def cleanup(): IO[Unit] =
    IO.blocking {
      val process = new ProcessBuilder(
        (List(config.isolateBin, s"--box-id=${state.boxId}") ++
          (if state.useCgroups then List("--cg") else Nil) ++
          List("--cleanup"))*
      ).start()
      process.waitFor(10, TimeUnit.SECONDS)
      ()
    }.void.handleError(_ => ())

object IsolateSandbox:
  def resource[A](config: AppConfig)(use: IsolateSandbox => IO[A]): IO[A] =
    initialize(config).bracket(use)(_.cleanup())

  private def initialize(config: AppConfig): IO[IsolateSandbox] =
    IO.blocking {
      val state =
        if config.preferIsolateCgroups then
          initializeAttempt(config, useCgroups = true).getOrElse(initializeUnsafe(config, useCgroups = false))
        else initializeUnsafe(config, useCgroups = false)
      new IsolateSandbox(state, config)
    }

  private def initializeAttempt(config: AppConfig, useCgroups: Boolean): Option[SandboxState] =
    scala.util.Try(initializeUnsafe(config, useCgroups)).toOption

  private def initializeUnsafe(config: AppConfig, useCgroups: Boolean): SandboxState =
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
    SandboxState(boxRoot = Path.of(boxRoot), boxId = config.isolateBoxId, useCgroups = useCgroups)

  private[judger] def readOptionalFile(path: Path): String =
    if Files.exists(path) then Files.readString(path, StandardCharsets.UTF_8) else ""

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

  private[judger] def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback
