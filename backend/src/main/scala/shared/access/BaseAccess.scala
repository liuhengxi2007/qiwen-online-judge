package shared.access



import io.circe.{Decoder, Encoder}

enum BaseAccess:
  case OwnerOnly
  case Public

object BaseAccess:
  def fromDatabase(value: String): Option[BaseAccess] =
    value match
      case "owner_only" => Some(BaseAccess.OwnerOnly)
      case "public" => Some(BaseAccess.Public)
      case _ => None

  def toDatabase(value: BaseAccess): String =
    value match
      case BaseAccess.OwnerOnly => "owner_only"
      case BaseAccess.Public => "public"

  given Encoder[BaseAccess] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[BaseAccess] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown base access: $value")
  }
