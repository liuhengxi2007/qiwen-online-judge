package domains.shared.model

import io.circe.{Decoder, Encoder}

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

  def fromDatabaseUnsafe(value: String): ResourceVisibility =
    fromDatabase(value).getOrElse(throw IllegalArgumentException(s"Unknown resource visibility: $value"))

  def toDatabase(value: ResourceVisibility): String =
    value match
      case ResourceVisibility.Private => "private"
      case ResourceVisibility.Group => "group"
      case ResourceVisibility.Public => "public"

  given Encoder[ResourceVisibility] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[ResourceVisibility] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown resource visibility: $value")
  }

enum ResourceStatus:
  case Draft
  case Published
  case Archived

object ResourceStatus:
  def fromDatabase(value: String): Option[ResourceStatus] =
    value match
      case "draft" => Some(ResourceStatus.Draft)
      case "published" => Some(ResourceStatus.Published)
      case "archived" => Some(ResourceStatus.Archived)
      case _ => None

  def fromDatabaseUnsafe(value: String): ResourceStatus =
    fromDatabase(value).getOrElse(throw IllegalArgumentException(s"Unknown resource status: $value"))

  def toDatabase(value: ResourceStatus): String =
    value match
      case ResourceStatus.Draft => "draft"
      case ResourceStatus.Published => "published"
      case ResourceStatus.Archived => "archived"

  given Encoder[ResourceStatus] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[ResourceStatus] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown resource status: $value")
  }
