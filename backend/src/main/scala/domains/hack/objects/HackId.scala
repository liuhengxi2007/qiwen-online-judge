package domains.hack.objects

import io.circe.{Decoder, Encoder}

import scala.util.Try

/** hack attempt 的公开 id；用于 URL、列表和 worker 上报。 */
final case class HackId(value: Long)

/** HackId 的 JSON 编解码与路径参数解析入口。 */
object HackId:
  given Encoder[HackId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[HackId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Hack id is invalid.") else Right(HackId(value))
  }

  /** 从字符串解析正整数 hack id；非法值返回业务错误。 */
  def parse(raw: String): Either[String, HackId] =
    Try(raw.trim.toLong)
      .toEither
      .left
      .map(_ => "Hack id is invalid.")
      .flatMap { value =>
        if value < 1 then Left("Hack id is invalid.")
        else Right(HackId(value))
      }
