package domains.usergroup.objects.request

import domains.usergroup.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)

object UpdateUserGroupMemberRoleRequest:
  given Encoder[UpdateUserGroupMemberRoleRequest] = deriveEncoder[UpdateUserGroupMemberRoleRequest]
  given Decoder[UpdateUserGroupMemberRoleRequest] = deriveDecoder[UpdateUserGroupMemberRoleRequest]
