package domains.blog.objects

import io.circe.{Decoder, Encoder}


enum BlogVisibility:
  case Public
  case Private

object BlogVisibility:
  given Encoder[BlogVisibility] = Encoder.encodeString.contramap(encode)
  given Decoder[BlogVisibility] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, BlogVisibility] =
    raw match
      case "public" => Right(BlogVisibility.Public)
      case "private" => Right(BlogVisibility.Private)
      case _ => Left("Blog visibility must be public or private.")

  private def encode(value: BlogVisibility): String =
    value match
      case BlogVisibility.Public => "public"
      case BlogVisibility.Private => "private"
