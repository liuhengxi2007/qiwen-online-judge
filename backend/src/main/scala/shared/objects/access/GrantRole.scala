package shared.objects.access

import io.circe.{Decoder, Encoder}

enum GrantRole:
  case Viewer
  case Manager

object GrantRole:
  given Encoder[GrantRole] = Encoder.encodeString.contramap(encode)
  given Decoder[GrantRole] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, GrantRole] =
    value.trim match
      case "viewer" => Right(GrantRole.Viewer)
      case "manager" => Right(GrantRole.Manager)
      case _ => Left("Grant role must be one of: viewer, manager.")

  private def encode(value: GrantRole): String =
    value match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"
