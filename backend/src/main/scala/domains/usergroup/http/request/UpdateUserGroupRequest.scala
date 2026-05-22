package domains.usergroup.http.request

import domains.usergroup.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateUserGroupRequest(
  name: UserGroupName,
  description: UserGroupDescription
)

object UpdateUserGroupRequest:
  given Encoder[UpdateUserGroupRequest] = deriveEncoder[UpdateUserGroupRequest]
  given Decoder[UpdateUserGroupRequest] = deriveDecoder[UpdateUserGroupRequest]
