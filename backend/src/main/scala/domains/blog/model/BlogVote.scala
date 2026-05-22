package domains.blog.model



import io.circe.{Decoder, Encoder}

enum BlogVote:
  case Up
  case Down

object BlogVote:
  def parse(raw: String): Either[String, BlogVote] =
    raw match
      case "up" => Right(BlogVote.Up)
      case "down" => Right(BlogVote.Down)
      case _ => Left("Blog vote must be up or down.")

  def toDatabase(vote: BlogVote): String =
    vote match
      case BlogVote.Up => "up"
      case BlogVote.Down => "down"

  def fromDatabase(value: String): Option[BlogVote] =
    value match
      case "up" => Some(BlogVote.Up)
      case "down" => Some(BlogVote.Down)
      case _ => None

  given Encoder[BlogVote] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[BlogVote] = Decoder.decodeString.emap(parse)
