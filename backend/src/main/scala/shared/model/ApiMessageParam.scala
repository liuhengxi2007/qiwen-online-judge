package shared.model



sealed trait ApiMessageParam

object ApiMessageParam:
  final case class Text(value: String) extends ApiMessageParam
  final case class IntValue(value: Int) extends ApiMessageParam
  final case class LongValue(value: Long) extends ApiMessageParam
  final case class BoolValue(value: Boolean) extends ApiMessageParam

type ApiMessageParams = Map[String, ApiMessageParam]
