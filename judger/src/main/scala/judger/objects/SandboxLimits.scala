package judger.objects

/** sandbox CPU 时间限制，单位毫秒。 */
final case class TimeLimitMs(value: Long) extends AnyVal
/** sandbox 墙钟时间限制，单位毫秒。 */
final case class WallTimeLimitMs(value: Long) extends AnyVal
/** sandbox 内存限制，单位 MB。 */
final case class MemoryLimitMb(value: Int) extends AnyVal
/** isolate 接收的内存限制，单位 KB。 */
final case class MemoryLimitKb(value: Long) extends AnyVal

/** sandbox 资源限制组合，统一 CPU、墙钟和内存边界。 */
final case class SandboxLimits(
  timeLimit: TimeLimitMs,
  wallTimeLimit: WallTimeLimitMs,
  memoryLimit: MemoryLimitMb
):
  /** 转换为 isolate 使用的 KB，并把过小内存限制抬升到最低 16MB。 */
  def memoryLimitKb: MemoryLimitKb =
    MemoryLimitKb(math.max(memoryLimit.value, 16).toLong * 1024L)

/** 提供常用资源限制构造函数，集中处理下限和默认墙钟倍率。 */
object SandboxLimits:
  /** 构造普通运行限制；墙钟时间按 CPU 时间的 1.5 倍再加 500ms。 */
  def runtime(timeLimitMs: Long, memoryLimitMb: Int): SandboxLimits =
    val normalizedTimeLimit = math.max(1L, timeLimitMs)
    runtimeWithWall(
      timeLimitMs = normalizedTimeLimit,
      wallTimeLimitMs = math.max(1L, (normalizedTimeLimit * 3 + 1) / 2 + 500L),
      memoryLimitMb = memoryLimitMb
    )

  /** 构造显式墙钟限制的运行配置；所有限制都会被规整到正数和最小内存。 */
  def runtimeWithWall(timeLimitMs: Long, wallTimeLimitMs: Long, memoryLimitMb: Int): SandboxLimits =
    SandboxLimits(
      timeLimit = TimeLimitMs(math.max(1L, timeLimitMs)),
      wallTimeLimit = WallTimeLimitMs(math.max(1L, wallTimeLimitMs)),
      memoryLimit = MemoryLimitMb(math.max(memoryLimitMb, 16))
    )
