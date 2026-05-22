package domains.usergroup.model



enum UserGroupRole:
  case Owner
  case Manager
  case Member

object UserGroupRole:
  def parse(value: String): Either[String, UserGroupRole] =
    value.trim match
      case "owner" => Right(UserGroupRole.Owner)
      case "manager" => Right(UserGroupRole.Manager)
      case "member" => Right(UserGroupRole.Member)
      case _ => Left("User group role must be one of: owner, manager, member.")
