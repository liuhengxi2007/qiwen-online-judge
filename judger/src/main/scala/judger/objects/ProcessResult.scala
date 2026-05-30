package judger.objects

final case class ProcessResult(
  exitCode: Option[Int],
  isolateStatus: Option[String],
  isolateMessage: Option[String],
  stdout: String,
  stderr: String,
  timedOut: Boolean,
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)
