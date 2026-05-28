package shared.objects.access

import io.circe.{Decoder, Encoder}

enum BaseAccess:
  case OwnerOnly
  case Public

object BaseAccess:
  given Encoder[BaseAccess] = Encoder.encodeString.contramap(encode)
  given Decoder[BaseAccess] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, BaseAccess] =
    value.trim match
      case "owner_only" => Right(BaseAccess.OwnerOnly)
      case "public" => Right(BaseAccess.Public)
      case _ => Left("Base access must be one of: owner_only, public.")

  private def encode(value: BaseAccess): String =
    value match
      case BaseAccess.OwnerOnly => "owner_only"
      case BaseAccess.Public => "public"
