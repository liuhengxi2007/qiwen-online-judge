package judger.objects

sealed trait SandboxStdin

object SandboxStdin:
  case object Empty extends SandboxStdin
  final case class Bytes(value: Array[Byte]) extends SandboxStdin
  final case class File(path: String) extends SandboxStdin

sealed trait SandboxStdout

object SandboxStdout:
  case object Capture extends SandboxStdout
  final case class File(path: String, capture: Boolean = true) extends SandboxStdout
  case object Discard extends SandboxStdout

final case class SandboxRunSpec(
  phase: String,
  command: RuntimeCommand,
  stdin: SandboxStdin,
  stdout: SandboxStdout,
  limits: SandboxLimits,
  boxOffset: Int = 0
)
