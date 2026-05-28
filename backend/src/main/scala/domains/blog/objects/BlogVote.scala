package domains.blog.objects

import io.circe.{Decoder, Encoder}


enum BlogVote:
  case Up
  case Down

object BlogVote:
  given Encoder[BlogVote] = Encoder.encodeString.contramap(encode)
  given Decoder[BlogVote] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, BlogVote] =
    raw match
      case "up" => Right(BlogVote.Up)
      case "down" => Right(BlogVote.Down)
      case _ => Left("Blog vote must be up or down.")

  private def encode(value: BlogVote): String =
    value match
      case BlogVote.Up => "up"
      case BlogVote.Down => "down"
