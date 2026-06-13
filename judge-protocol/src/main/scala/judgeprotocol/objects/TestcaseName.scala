package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 测试点名称的协议值对象，用于配置和展示中的短标签。 */
final case class TestcaseName(value: String)

/** 校验测试点名称的存在性和长度边界。 */
object TestcaseName:
  /** 规范化外部输入为测试点名称；失败时返回可展示的校验错误。 */
  def parse(raw: String): Either[String, TestcaseName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Testcase name is required.")
    else if normalized.length > 120 then Left("Testcase name must be at most 120 characters.")
    else Right(TestcaseName(normalized))

  given Encoder[TestcaseName] = Encoder.encodeString.contramap(_.value)
  given Decoder[TestcaseName] = Decoder.decodeString.emap(parse)
