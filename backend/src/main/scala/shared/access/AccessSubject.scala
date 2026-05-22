package shared.access



import domains.auth.model.Username
import domains.usergroup.model.UserGroupSlug
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

enum AccessSubject:
  case User(username: Username)
  case UserGroup(slug: UserGroupSlug)

object AccessSubject:
  def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value

  given Encoder[AccessSubject] = Encoder.instance {
    case AccessSubject.User(username) =>
      Json.obj(
        "kind" -> Json.fromString("user"),
        "username" -> username.asJson
      )
    case AccessSubject.UserGroup(slug) =>
      Json.obj(
        "kind" -> Json.fromString("user_group"),
        "slug" -> slug.asJson
      )
  }

  given Decoder[AccessSubject] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "user" =>
        cursor.downField("username").as[Username].map(AccessSubject.User(_))
      case "user_group" =>
        cursor.downField("slug").as[UserGroupSlug].map(AccessSubject.UserGroup(_))
      case other =>
        Left(DecodingFailure(s"Unknown access subject kind: $other", cursor.history))
    }
  }
