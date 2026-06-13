package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** backend 分配给一个 judger 租约的标识；由 worker 心跳和 claim 请求携带。 */
final case class JudgerId(value: String)

/** 校验并编解码 judger 标识，防止空值和过长值进入 worker 协议。 */
object JudgerId:
  /** 从外部输入构造 judger 标识；返回错误字符串而不是抛异常，供 HTTP 解码复用。 */
  def parse(raw: String): Either[String, JudgerId] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Judger id is required.")
    else if normalized.length > 120 then Left("Judger id must be at most 120 characters.")
    else Right(JudgerId(normalized))

  given Encoder[JudgerId] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgerId] = Decoder.decodeString.emap(parse)
