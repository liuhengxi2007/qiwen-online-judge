package shared.objects.access

import io.circe.{Decoder, Encoder}

enum BaseAccess:
  case Restricted
  case Public

object BaseAccess:
  given Encoder[BaseAccess] = Encoder.encodeString.contramap(encode)
  given Decoder[BaseAccess] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, BaseAccess] =
    value.trim match
      case "restricted" | "owner_only" => Right(BaseAccess.Restricted)
      case "public" => Right(BaseAccess.Public)
      case _ => Left("Base access must be one of: restricted, public.")

  private def encode(value: BaseAccess): String =
    value match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"
