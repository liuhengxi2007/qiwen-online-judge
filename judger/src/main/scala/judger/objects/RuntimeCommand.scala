package judger.objects

final case class RuntimeCommand(
  command: String,
  args: List[String],
  processLimit: Int
)
