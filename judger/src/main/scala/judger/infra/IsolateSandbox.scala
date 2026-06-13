package judger.infra

import cats.effect.IO
import judger.config.AppConfig
import judger.objects.{ProcessResult, SandboxRunSpec, SandboxStdin, SandboxStdout}

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

/** isolate box 的已初始化状态，记录 box id 以及是否使用 cgroups。 */
private final case class SandboxState(
  boxId: Int,
  useCgroups: Boolean
)

/** sandbox 执行接口，隐藏 isolate 具体实现，便于测试点 runner 只依赖运行语义。 */
trait SandboxSession:
  /** 在指定宿主工作目录下执行一次 sandbox 任务，并返回统一进程结果。 */
  def run(spec: SandboxRunSpec, hostWorkingDirectory: Path): IO[ProcessResult]

/** 基于 isolate 的 sandbox，会把工作目录绑定为 /box 并按资源限制运行命令。 */
final class IsolateSandbox private (private val state: SandboxState, config: AppConfig) extends SandboxSession:
  /** 执行 sandbox 任务；boxOffset 非零时会临时初始化额外 box 供并发交互进程使用。 */
  override def run(spec: SandboxRunSpec, hostWorkingDirectory: Path): IO[ProcessResult] =
    if spec.boxOffset == 0 then runInCurrentBox(spec, hostWorkingDirectory)
    else
      val boxId = IsolateSandbox.boxIdAt(state.boxId, spec.boxOffset)
      IsolateSandbox
        .initialize(config, boxId, state.useCgroups)
        .bracket(tempState => new IsolateSandbox(tempState, config).runInCurrentBox(spec.copy(boxOffset = 0), hostWorkingDirectory))(tempState =>
          IsolateSandbox.cleanup(config, tempState)
        )

  private def runInCurrentBox(spec: SandboxRunSpec, hostWorkingDirectory: Path): IO[ProcessResult] =
    IO.blocking {
      val safePhase = IsolateSandbox.sanitizeFilename(spec.phase)
      val metaPath = hostWorkingDirectory.resolve(s"$safePhase.meta")
      val defaultStdinPath = hostWorkingDirectory.resolve(s"$safePhase.stdin")
      val (stdoutFile, captureStdout) =
        spec.stdout match
          case SandboxStdout.Capture => s"$safePhase.stdout" -> true
          case SandboxStdout.File(path, capture) => path -> capture
          case SandboxStdout.Discard => s"$safePhase.stdout" -> false
      val stdoutPath = hostWorkingDirectory.resolve(stdoutFile)
      val stderrPath = hostWorkingDirectory.resolve(s"$safePhase.stderr")
      Files.deleteIfExists(metaPath)
      spec.stdin match
        case SandboxStdin.Bytes(bytes) => Files.write(defaultStdinPath, bytes)
        case _ => Files.deleteIfExists(defaultStdinPath)
      spec.stdout match
        case SandboxStdout.File(_, _) => ()
        case _ => Files.deleteIfExists(stdoutPath)
      Files.deleteIfExists(stderrPath)
      val stdinArgs =
        spec.stdin match
          case SandboxStdin.Empty => Nil
          case SandboxStdin.Bytes(_) => List(s"--stdin=${defaultStdinPath.getFileName}")
          case SandboxStdin.File(path) => List(s"--stdin=$path")

      val isolateArgs =
        List(
          config.isolateBin,
          s"--box-id=${state.boxId}",
          s"--dir=/box=${hostWorkingDirectory.toAbsolutePath}:rw",
          s"--meta=${metaPath.toAbsolutePath}",
          s"--stdout=$stdoutFile",
          s"--stderr=${stderrPath.getFileName}",
          "--chdir=/box",
          s"--time=${IsolateSandbox.secondsCeil(spec.limits.timeLimit.value)}",
          s"--wall-time=${IsolateSandbox.secondsCeil(spec.limits.wallTimeLimit.value)}",
          s"--mem=${spec.limits.memoryLimitKb.value}"
        ) ++ stdinArgs ++
          (
          (if state.useCgroups then List("--cg") else Nil) ++
          List(s"--processes=${math.max(spec.command.processLimit, 1)}") ++
          List("--run", "--", spec.command.command) ++ spec.command.args
          )

      val builder = new ProcessBuilder(isolateArgs*)
      builder.directory(hostWorkingDirectory.toFile)
      val process = builder.start()
      process.getOutputStream.close()

      val completed = process.waitFor(spec.limits.wallTimeLimit.value + 5000L, TimeUnit.MILLISECONDS)
      if !completed then
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)

      val launcherStdout = IsolateSandbox.readStream(process.getInputStream)
      val launcherStderr = IsolateSandbox.readStream(process.getErrorStream)
      val meta = IsolateSandbox.readMeta(metaPath)
      val stdout = if captureStdout then IsolateSandbox.readOptionalFile(stdoutPath) else ""
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

  private[judger] def cleanup(): IO[Unit] =
    IsolateSandbox.cleanup(config, state)

/** 管理 isolate 初始化、清理和 meta 文件解析。 */
object IsolateSandbox:
  /** 以 Resource 风格初始化 isolate，并在使用结束后清理 box。 */
  def resource[A](config: AppConfig)(use: IsolateSandbox => IO[A]): IO[A] =
    initialize(config).bracket(use)(_.cleanup())

  private def initialize(config: AppConfig): IO[IsolateSandbox] =
    IO.blocking {
      val state =
        if config.preferIsolateCgroups then
          // 注意：优先使用 cgroups，但本地 isolate 未配置 cgroups 时回退到普通模式，避免开发环境无法启动。
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
    // 注意：cleanup 处于资源释放路径，失败时吞掉异常以避免覆盖原始判题错误。
    }.void.handleError(_ => ())

  /** 读取可选文本文件；缺失或非普通文件按空输出处理。 */
  private[judger] def readOptionalFile(path: Path): String =
    if Files.exists(path) && Files.isRegularFile(path) then Files.readString(path, StandardCharsets.UTF_8) else ""

  /** 解析 isolate meta 文件中的 key:value 行，无法解析的行会被忽略。 */
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

  /** 从 isolate meta 中读取 CPU 时间并换算为毫秒。 */
  private[judger] def timeUsedMs(meta: Map[String, String]): Option[Long] =
    meta
      .get("time")
      .flatMap(value => scala.util.Try(BigDecimal(value)).toOption)
      .map(seconds => math.max(0L, (seconds * BigDecimal(1000)).setScale(0, BigDecimal.RoundingMode.HALF_UP).toLong))

  /** 从 isolate meta 中读取墙钟时间并换算为毫秒。 */
  private[judger] def wallTimeUsedMs(meta: Map[String, String]): Option[Long] =
    meta
      .get("time-wall")
      .flatMap(value => scala.util.Try(BigDecimal(value)).toOption)
      .map(seconds => math.max(0L, (seconds * BigDecimal(1000)).setScale(0, BigDecimal.RoundingMode.HALF_UP).toLong))

  /** 从 isolate meta 中读取最大 RSS，单位保持为 KB。 */
  private[judger] def memoryUsedKb(meta: Map[String, String]): Option[Long] =
    meta.get("max-rss").flatMap(_.toLongOption).map(value => math.max(0L, value))

  /** 读完进程输出流并按 UTF-8 解码。 */
  private[judger] def readStream(stream: java.io.InputStream): String =
    val buffer = ByteArrayOutputStream()
    stream.transferTo(buffer)
    buffer.toString(StandardCharsets.UTF_8)

  /** 将 phase 或 role 文本规整为本地文件名片段，避免写出分隔符。 */
  private[judger] def sanitizeFilename(value: String): String =
    value.map {
      case current if current.isLetterOrDigit => current
      case _ => '_'
    }

  /** 将毫秒上取整为 isolate 接收的秒级限制，最小为 1 秒。 */
  private[judger] def secondsCeil(milliseconds: Long): Long =
    math.max(1L, (milliseconds + 999L) / 1000L)

  /** 根据基础 box id 和并发偏移计算 isolate box id，限制在 0-999。 */
  private[judger] def boxIdAt(baseBoxId: Int, offset: Int): Int =
    Math.floorMod(baseBoxId + offset, 1000)

  /** 从两个候选输出中选择第一段非空文本，否则返回兜底消息。 */
  private[judger] def nonEmptyOrFallback(primary: String, secondary: String, fallback: String): String =
    val first = primary.trim
    if first.nonEmpty then first
    else
      val second = secondary.trim
      if second.nonEmpty then second else fallback
