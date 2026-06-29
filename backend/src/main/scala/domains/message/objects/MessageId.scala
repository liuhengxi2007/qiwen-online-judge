package domains.message.objects

import io.circe.{Decoder, Encoder}


import java.util.UUID
import scala.util.Try

/** 私信消息 id 领域值，封装 UUID。 */
final case class MessageId(value: UUID)

/** 提供消息 id 的字符串 JSON codec 和路径解析。 */
object MessageId:
  given Encoder[MessageId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[MessageId] = Decoder.decodeString.emap(parse)

  /** 将路径或查询字符串解析为 UUID 消息 id。 */
  def parse(raw: String): Either[String, MessageId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(MessageId(_))
