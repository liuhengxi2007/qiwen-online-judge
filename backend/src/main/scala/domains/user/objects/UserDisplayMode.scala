package domains.user.objects

import io.circe.{Decoder, Encoder}


enum UserDisplayMode:
  case DisplayName
  case Username
  case DisplayNameWithUsername

object UserDisplayMode:
  given Encoder[UserDisplayMode] = Encoder.encodeString.contramap(encode)
  given Decoder[UserDisplayMode] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, UserDisplayMode] =
    value.trim match
      case "display_name" => Right(UserDisplayMode.DisplayName)
      case "username" => Right(UserDisplayMode.Username)
      case "display_name_with_username" => Right(UserDisplayMode.DisplayNameWithUsername)
      case _ =>
        Left("User display mode must be one of: display_name, username, display_name_with_username.")

  private def encode(value: UserDisplayMode): String =
    value match
      case UserDisplayMode.DisplayName => "display_name"
      case UserDisplayMode.Username => "username"
      case UserDisplayMode.DisplayNameWithUsername => "display_name_with_username"
