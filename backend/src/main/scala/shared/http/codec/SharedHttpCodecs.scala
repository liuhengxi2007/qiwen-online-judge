package shared.http.codec

import domains.user.model.Username
import domains.usergroup.model.UserGroupSlug
import shared.access.*
import shared.model.*
import shared.upload.StoredFilePath
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object SharedHttpCodecs:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ApiMessageParam] = Encoder.instance {
    case ApiMessageParam.Text(value) =>
      Json.obj("kind" -> Json.fromString("text"), "value" -> Json.fromString(value))
    case ApiMessageParam.IntValue(value) =>
      Json.obj("kind" -> Json.fromString("int"), "value" -> Json.fromInt(value))
    case ApiMessageParam.LongValue(value) =>
      Json.obj("kind" -> Json.fromString("long"), "value" -> Json.fromLong(value))
    case ApiMessageParam.BoolValue(value) =>
      Json.obj("kind" -> Json.fromString("bool"), "value" -> Json.fromBoolean(value))
  }

  given Decoder[ApiMessageParam] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "text" => cursor.downField("value").as[String].map(ApiMessageParam.Text(_))
      case "int" => cursor.downField("value").as[Int].map(ApiMessageParam.IntValue(_))
      case "long" => cursor.downField("value").as[Long].map(ApiMessageParam.LongValue(_))
      case "bool" => cursor.downField("value").as[Boolean].map(ApiMessageParam.BoolValue(_))
      case other => Left(DecodingFailure(s"Unsupported ApiMessageParam kind: $other", cursor.history))
    }
  }

  given Encoder[AuditFields] = deriveEncoder[AuditFields]
  given Decoder[AuditFields] = deriveDecoder[AuditFields]

  given Encoder[PageRequest] = deriveEncoder[PageRequest]
  given Decoder[PageRequest] = deriveDecoder[PageRequest]
  given [A: Encoder]: Encoder[PageResponse[A]] = deriveEncoder[PageResponse[A]]
  given [A: Decoder]: Decoder[PageResponse[A]] = deriveDecoder[PageResponse[A]]

  given Encoder[ResourceVisibility] = Encoder.encodeString.contramap(encodeResourceVisibility)
  given Decoder[ResourceVisibility] = Decoder.decodeString.emap { value =>
    ResourceVisibility.parse(value)
  }

  given Encoder[BaseAccess] = Encoder.encodeString.contramap(encodeBaseAccess)
  given Decoder[BaseAccess] = Decoder.decodeString.emap(BaseAccess.parse)

  given Encoder[GrantRole] = Encoder.encodeString.contramap(encodeGrantRole)
  given Decoder[GrantRole] = Decoder.decodeString.emap(GrantRole.parse)

  given Encoder[ResourceKind] = Encoder.encodeString.contramap(encodeResourceKind)
  given Decoder[ResourceKind] = Decoder.decodeString.emap(ResourceKind.parse)

  given Encoder[ResourceId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ResourceId] = Decoder.decodeString.emap(ResourceId.parse)

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
          Username.parse(value)
            .left
            .map(message => DecodingFailure(message, cursor.downField("username").history))
            .map(AccessSubject.User(_))
        }
      case "user_group" =>
        cursor.downField("slug").as[String].flatMap { value =>
          UserGroupSlug.parse(value).left.map(message => DecodingFailure(message, cursor.history)).map(AccessSubject.UserGroup(_))
        }
      case other =>
        Left(DecodingFailure(s"Unknown access subject kind: $other", cursor.history))
    }
  }

  given Encoder[ResourceAccessPolicy] = deriveEncoder[ResourceAccessPolicy]
  given Decoder[ResourceAccessPolicy] = deriveDecoder[ResourceAccessPolicy]

  given Encoder[ResourceAccessGrant] = deriveEncoder[ResourceAccessGrant]
  given Decoder[ResourceAccessGrant] = deriveDecoder[ResourceAccessGrant]

  given Encoder[StoredFilePath] = Encoder.encodeString.contramap(_.value)
  given Decoder[StoredFilePath] = Decoder.decodeString.emap(StoredFilePath.parse)

  private def encodeResourceVisibility(value: ResourceVisibility): String =
    value match
      case ResourceVisibility.Private => "private"
      case ResourceVisibility.Group => "group"
      case ResourceVisibility.Public => "public"

  private def encodeBaseAccess(value: BaseAccess): String =
    value match
      case BaseAccess.OwnerOnly => "owner_only"
      case BaseAccess.Public => "public"

  private def encodeGrantRole(value: GrantRole): String =
    value match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"

  private def encodeResourceKind(value: ResourceKind): String =
    value match
      case ResourceKind.Problem => "problem"
      case ResourceKind.ProblemSet => "problem_set"
