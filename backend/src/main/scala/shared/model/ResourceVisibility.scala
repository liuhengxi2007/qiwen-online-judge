package shared.model

enum ResourceVisibility:
  case Private
  case Group
  case Public

object ResourceVisibility:
  def parse(value: String): Either[String, ResourceVisibility] =
    value.trim match
      case "private" => Right(ResourceVisibility.Private)
      case "group" => Right(ResourceVisibility.Group)
      case "public" => Right(ResourceVisibility.Public)
      case _ => Left("Resource visibility must be one of: private, group, public.")
