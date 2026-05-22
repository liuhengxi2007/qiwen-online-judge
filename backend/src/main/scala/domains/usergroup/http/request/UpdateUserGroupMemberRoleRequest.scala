package domains.usergroup.http.request

import domains.usergroup.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)

object UpdateUserGroupMemberRoleRequest:
  given Encoder[UpdateUserGroupMemberRoleRequest] = deriveEncoder[UpdateUserGroupMemberRoleRequest]
  given Decoder[UpdateUserGroupMemberRoleRequest] = deriveDecoder[UpdateUserGroupMemberRoleRequest]
