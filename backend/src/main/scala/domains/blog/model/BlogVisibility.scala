package domains.blog.model

import io.circe.{Decoder, Encoder}

enum BlogVisibility:
  case Public
  case Private

object BlogVisibility:
  def parse(raw: String): Either[String, BlogVisibility] =
    raw match
      case "public" => Right(BlogVisibility.Public)
      case "private" => Right(BlogVisibility.Private)
      case _ => Left("Blog visibility must be public or private.")

  def toDatabase(visibility: BlogVisibility): String =
    visibility match
      case BlogVisibility.Public => "public"
      case BlogVisibility.Private => "private"

  def fromDatabase(value: String): Option[BlogVisibility] =
    value match
      case "public" => Some(BlogVisibility.Public)
      case "private" => Some(BlogVisibility.Private)
      case _ => None

  given Encoder[BlogVisibility] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[BlogVisibility] = Decoder.decodeString.emap(parse)
