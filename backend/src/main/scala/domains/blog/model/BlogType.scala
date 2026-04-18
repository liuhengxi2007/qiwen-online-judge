package domains.blog.model

import io.circe.{Decoder, Encoder}

enum BlogType:
  case General
  case Problem

object BlogType:
  def parse(raw: String): Either[String, BlogType] =
    raw match
      case "general" => Right(BlogType.General)
      case "problem" => Right(BlogType.Problem)
      case _ => Left("Blog type must be general or problem.")

  def toDatabase(blogType: BlogType): String =
    blogType match
      case BlogType.General => "general"
      case BlogType.Problem => "problem"

  def fromDatabase(value: String): Option[BlogType] =
    value match
      case "general" => Some(BlogType.General)
      case "problem" => Some(BlogType.Problem)
      case _ => None

  given Encoder[BlogType] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[BlogType] = Decoder.decodeString.emap(parse)
