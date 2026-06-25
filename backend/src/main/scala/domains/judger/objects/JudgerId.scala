package domains.judger.objects

import io.circe.{Decoder, Encoder}

/** judger 租约标识；管理端 API 使用该类型展示已注册 worker。 */
final case class JudgerId(value: String)

/** JudgerId 的 JSON 编解码和格式校验入口。 */
object JudgerId:
  given Encoder[JudgerId] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgerId] = Decoder.decodeString.emap(parse)

  /** 校验 judger id，保持与 worker 协议 JudgerId 相同的长度和空值规则。 */
  def parse(raw: String): Either[String, JudgerId] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Judger id is required.")
    else if normalized.length > 120 then Left("Judger id must be at most 120 characters.")
    else Right(JudgerId(normalized))
