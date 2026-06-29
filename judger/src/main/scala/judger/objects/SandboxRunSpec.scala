package judger.objects

/** sandbox 标准输入来源，描述空输入、内存字节或 box 内文件路径。 */
sealed trait SandboxStdin

/** 构造 sandbox 标准输入来源；File 路径应由调用方保证位于 sandbox 可见目录内。 */
object SandboxStdin:
  case object Empty extends SandboxStdin
  final case class Bytes(value: Array[Byte]) extends SandboxStdin
  final case class File(path: String) extends SandboxStdin

/** sandbox 标准输出处理方式，描述捕获、写入文件或丢弃。 */
sealed trait SandboxStdout

/** 构造 sandbox 标准输出策略；File 可选择是否同时读回内容。 */
object SandboxStdout:
  case object Capture extends SandboxStdout
  final case class File(path: String, capture: Boolean = true) extends SandboxStdout
  case object Discard extends SandboxStdout

/** 一次 sandbox 运行的完整规格，包含命令、输入输出、资源限制和可选 box 偏移。 */
final case class SandboxRunSpec(
  phase: String,
  command: RuntimeCommand,
  stdin: SandboxStdin,
  stdout: SandboxStdout,
  limits: SandboxLimits,
  boxOffset: Int = 0
)
