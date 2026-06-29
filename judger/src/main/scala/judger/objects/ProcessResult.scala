package judger.objects

/** 进程或 sandbox 执行后的统一结果，包含退出码、输出、超时状态和资源用量。 */
final case class ProcessResult(
  exitCode: Option[Int],
  isolateStatus: Option[String],
  isolateMessage: Option[String],
  stdout: String,
  stderr: String,
  timedOut: Boolean,
  timeUsedMs: Option[Long],
  wallTimeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)
