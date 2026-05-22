package domains.usergroup.http.request

import domains.usergroup.model.*

import domains.auth.model.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AddUserGroupMemberRequest(
  username: Username,
  role: AddUserGroupMemberRole
)

object AddUserGroupMemberRequest:
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]
