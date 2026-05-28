package shared.objects

import io.circe.{Decoder, Encoder}

enum ResourceVisibility:
  case Private
  case Group
  case Public

object ResourceVisibility:
  given Encoder[ResourceVisibility] = Encoder.encodeString.contramap(encode)
  given Decoder[ResourceVisibility] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, ResourceVisibility] =
    value.trim match
      case "private" => Right(ResourceVisibility.Private)
      case "group" => Right(ResourceVisibility.Group)
      case "public" => Right(ResourceVisibility.Public)
      case _ => Left("Resource visibility must be one of: private, group, public.")

  private def encode(value: ResourceVisibility): String =
    value match
      case ResourceVisibility.Private => "private"
      case ResourceVisibility.Group => "group"
      case ResourceVisibility.Public => "public"
