package judger.infra

final case class TimeLimitMs(value: Long) extends AnyVal
final case class WallTimeLimitMs(value: Long) extends AnyVal
final case class MemoryLimitMb(value: Int) extends AnyVal
final case class MemoryLimitKb(value: Long) extends AnyVal

final case class SandboxLimits(
  timeLimit: TimeLimitMs,
  wallTimeLimit: WallTimeLimitMs,
  memoryLimit: MemoryLimitMb
):
  def memoryLimitKb: MemoryLimitKb =
    MemoryLimitKb(math.max(memoryLimit.value, 16).toLong * 1024L)

object SandboxLimits:
  def runtime(timeLimitMs: Long, memoryLimitMb: Int): SandboxLimits =
    val normalizedTimeLimit = math.max(1L, timeLimitMs)
    SandboxLimits(
      timeLimit = TimeLimitMs(normalizedTimeLimit),
      wallTimeLimit = WallTimeLimitMs(math.max(1L, (normalizedTimeLimit * 3 + 1) / 2 + 500L)),
      memoryLimit = MemoryLimitMb(math.max(memoryLimitMb, 16))
    )
