package shared.access

enum GrantRole:
  case Viewer
  case Manager

object GrantRole:
  def parse(value: String): Either[String, GrantRole] =
    value.trim match
      case "viewer" => Right(GrantRole.Viewer)
      case "manager" => Right(GrantRole.Manager)
      case _ => Left("Grant role must be one of: viewer, manager.")
