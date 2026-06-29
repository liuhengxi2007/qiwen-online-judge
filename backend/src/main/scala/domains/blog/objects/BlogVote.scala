package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客和评论投票方向枚举。 */
enum BlogVote:
  case Up
  case Down

/** 提供投票方向线格式 codec。 */
object BlogVote:
  given Encoder[BlogVote] = Encoder.encodeString.contramap(encode)
  given Decoder[BlogVote] = Decoder.decodeString.emap(parse)

  /** 解析数据库/API 线值，只接受 up 或 down。 */
  def parse(raw: String): Either[String, BlogVote] =
    raw match
      case "up" => Right(BlogVote.Up)
      case "down" => Right(BlogVote.Down)
      case _ => Left("Blog vote must be up or down.")

  private def encode(value: BlogVote): String =
    value match
      case BlogVote.Up => "up"
      case BlogVote.Down => "down"
