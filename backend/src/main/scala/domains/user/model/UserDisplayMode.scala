package domains.user.model



enum UserDisplayMode:
  case DisplayName
  case Username
  case DisplayNameWithUsername

object UserDisplayMode:
  def parse(value: String): Either[String, UserDisplayMode] =
    value.trim match
      case "display_name" => Right(UserDisplayMode.DisplayName)
      case "username" => Right(UserDisplayMode.Username)
      case "display_name_with_username" => Right(UserDisplayMode.DisplayNameWithUsername)
      case _ =>
        Left("User display mode must be one of: display_name, username, display_name_with_username.")
