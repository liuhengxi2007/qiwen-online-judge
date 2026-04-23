package domains.shared.access

import io.circe.{Decoder, Encoder}

enum GrantRole:
  case Viewer
  case Manager

object GrantRole:
  def fromDatabase(value: String): Option[GrantRole] =
    value match
      case "viewer" => Some(GrantRole.Viewer)
      case "manager" => Some(GrantRole.Manager)
      case _ => None

  def toDatabase(value: GrantRole): String =
    value match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"

  given Encoder[GrantRole] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[GrantRole] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown grant role: $value")
  }
