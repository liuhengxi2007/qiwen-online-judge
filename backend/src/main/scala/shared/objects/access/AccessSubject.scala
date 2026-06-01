package shared.objects.access

import io.circe.{Decoder, DecodingFailure, Encoder, Json}

enum AccessSubject:
  case User(username: AccessUsername)
  case UserGroup(slug: AccessUserGroupSlug)

object AccessSubject:
  given Encoder[AccessSubject] = Encoder.instance {
    case AccessSubject.User(username) =>
      Json.obj(
        "kind" -> Json.fromString("user"),
        "username" -> Json.fromString(username.value)
      )
    case AccessSubject.UserGroup(slug) =>
      Json.obj(
        "kind" -> Json.fromString("user_group"),
        "slug" -> Json.fromString(slug.value)
      )
  }

  given Decoder[AccessSubject] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "user" =>
        cursor.downField("username").as[String].flatMap { value =>
          AccessUsername
            .parse(value)
            .left
            .map(message => DecodingFailure(message, cursor.downField("username").history))
            .map(AccessSubject.User(_))
        }
      case "user_group" =>
        cursor.downField("slug").as[String].flatMap { value =>
          AccessUserGroupSlug
            .parse(value)
            .left
            .map(message => DecodingFailure(message, cursor.history))
            .map(AccessSubject.UserGroup(_))
        }
      case other =>
        Left(DecodingFailure(s"Unknown access subject kind: $other", cursor.history))
    }
  }
