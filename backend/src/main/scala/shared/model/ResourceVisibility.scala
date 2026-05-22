package shared.model

enum ResourceVisibility:
  case Private
  case Group
  case Public

object ResourceVisibility:
  def fromDatabase(value: String): Option[ResourceVisibility] =
    value match
      case "private" => Some(ResourceVisibility.Private)
      case "group" => Some(ResourceVisibility.Group)
      case "public" => Some(ResourceVisibility.Public)
      case _ => None

  def toDatabase(value: ResourceVisibility): String =
    value match
      case ResourceVisibility.Private => "private"
      case ResourceVisibility.Group => "group"
      case ResourceVisibility.Public => "public"
