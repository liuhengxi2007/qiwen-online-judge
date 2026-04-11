package domains.shared.access

import domains.auth.model.Username
import domains.usergroup.model.UserGroupSlug
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import java.time.Instant
import java.util.UUID
import scala.util.Try

final case class ResourceId(value: UUID)

object ResourceId:
  def random(): ResourceId = ResourceId(UUID.randomUUID())

  given Encoder[ResourceId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[ResourceId] = Decoder.decodeString.emap { value =>
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ResourceId(_))
  }

enum ResourceKind:
  case Problem
  case ProblemSet

object ResourceKind:
  def fromDatabase(value: String): Option[ResourceKind] =
    value match
      case "problem" => Some(ResourceKind.Problem)
      case "problem_set" => Some(ResourceKind.ProblemSet)
      case _ => None

  def toDatabase(value: ResourceKind): String =
    value match
      case ResourceKind.Problem => "problem"
      case ResourceKind.ProblemSet => "problem_set"

  given Encoder[ResourceKind] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[ResourceKind] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown resource kind: $value")
  }

enum BaseAccess:
  case OwnerOnly
  case Public

object BaseAccess:
  def fromDatabase(value: String): Option[BaseAccess] =
    value match
      case "owner_only" => Some(BaseAccess.OwnerOnly)
      case "public" => Some(BaseAccess.Public)
      case _ => None

  def toDatabase(value: BaseAccess): String =
    value match
      case BaseAccess.OwnerOnly => "owner_only"
      case BaseAccess.Public => "public"

  given Encoder[BaseAccess] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[BaseAccess] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown base access: $value")
  }

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

final case class ResourceAccessPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject],
  managerGrants: List[AccessSubject]
)

object ResourceAccessPolicy:
  given Encoder[ResourceAccessPolicy] = deriveEncoder[ResourceAccessPolicy]
  given Decoder[ResourceAccessPolicy] = deriveDecoder[ResourceAccessPolicy]

enum GrantRole:
  case Viewer
  case Manager

object GrantRole:
  def fromDatabase(value: String): Option[GrantRole] =
    value match
      case "viewer" => Some(GrantRole.Viewer)
      case "manager" => Some(GrantRole.Manager)
      case _ => None

  def toDatabase(value: GrantRole): String =
    value match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"

  given Encoder[GrantRole] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[GrantRole] = Decoder.decodeString.emap { value =>
    fromDatabase(value).toRight(s"Unknown grant role: $value")
  }

final case class ResourceAccessGrant(
  resourceKind: ResourceKind,
  resourceId: ResourceId,
  grantRole: GrantRole,
  subject: AccessSubject,
  createdAt: Instant
)

object ResourceAccessGrant:
  private given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ResourceAccessGrant] = deriveEncoder[ResourceAccessGrant]
  given Decoder[ResourceAccessGrant] = deriveDecoder[ResourceAccessGrant]

object AccessPolicyEvaluator:
  def canView(
    policy: ResourceAccessPolicy,
    viewerUsername: Username,
    viewerGroupSlugs: Set[UserGroupSlug],
    isOwner: Boolean,
    hasGlobalOverride: Boolean
  ): Boolean =
    isOwner ||
      hasGlobalOverride ||
      policy.baseAccess == BaseAccess.Public ||
      policy.viewerGrants.exists {
        case AccessSubject.User(username) =>
          username.value == viewerUsername.value
        case AccessSubject.UserGroup(slug) =>
          viewerGroupSlugs.contains(slug)
      }

  def canManage(
    policy: ResourceAccessPolicy,
    actorUsername: Username,
    actorGroupSlugs: Set[UserGroupSlug],
    hasGlobalOverride: Boolean
  ): Boolean =
    hasGlobalOverride ||
      policy.managerGrants.exists {
        case AccessSubject.User(username) =>
          username.value == actorUsername.value
        case AccessSubject.UserGroup(slug) =>
          actorGroupSlugs.contains(slug)
      }
