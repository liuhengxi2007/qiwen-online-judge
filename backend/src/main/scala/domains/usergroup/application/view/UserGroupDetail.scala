package domains.usergroup.application.view

import domains.usergroup.model.*

import domains.auth.model.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class UserGroupDetail(
  id: UserGroupId,
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription,
  ownerUsername: Username,
  members: List[UserGroupMember],
  createdAt: Instant,
  updatedAt: Instant
)

object UserGroupDetail:
  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserGroupDetail] = deriveEncoder[UserGroupDetail]
  given Decoder[UserGroupDetail] = deriveDecoder[UserGroupDetail]
