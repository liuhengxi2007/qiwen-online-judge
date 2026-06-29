package judger.objects

/** 可在 sandbox 内执行的命令描述，command 通常是 /box 内路径或 sandbox 可见的绝对路径。 */
final case class RuntimeCommand(
  command: String,
  args: List[String],
  processLimit: Int
)
