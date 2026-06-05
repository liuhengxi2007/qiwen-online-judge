package judger.objects

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
    runtimeWithWall(
      timeLimitMs = normalizedTimeLimit,
      wallTimeLimitMs = math.max(1L, (normalizedTimeLimit * 3 + 1) / 2 + 500L),
      memoryLimitMb = memoryLimitMb
    )

  def runtimeWithWall(timeLimitMs: Long, wallTimeLimitMs: Long, memoryLimitMb: Int): SandboxLimits =
    SandboxLimits(
      timeLimit = TimeLimitMs(math.max(1L, timeLimitMs)),
      wallTimeLimit = WallTimeLimitMs(math.max(1L, wallTimeLimitMs)),
      memoryLimit = MemoryLimitMb(math.max(memoryLimitMb, 16))
    )

  def realTime(realTimeMs: Long, memoryLimitMb: Int): SandboxLimits =
    runtimeWithWall(
      timeLimitMs = realTimeMs,
      wallTimeLimitMs = realTimeMs,
      memoryLimitMb = memoryLimitMb
    )
