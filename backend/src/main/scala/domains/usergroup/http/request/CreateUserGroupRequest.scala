package domains.usergroup.http.request

import domains.usergroup.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateUserGroupRequest(
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription
)

object CreateUserGroupRequest:
  given Encoder[CreateUserGroupRequest] = deriveEncoder[CreateUserGroupRequest]
  given Decoder[CreateUserGroupRequest] = deriveDecoder[CreateUserGroupRequest]
