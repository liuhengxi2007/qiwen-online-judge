package judger.objects

final case class SandboxExecutionRequest(
  phase: String,
  command: String,
  args: List[String],
  stdin: Option[Array[Byte]],
  limits: SandboxLimits,
  processLimit: Int
)
