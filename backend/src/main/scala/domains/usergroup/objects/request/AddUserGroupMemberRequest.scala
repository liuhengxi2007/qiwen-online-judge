package domains.usergroup.objects.request

import domains.usergroup.objects.*

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AddUserGroupMemberRequest(
  username: Username,
  role: AddUserGroupMemberRole
)

object AddUserGroupMemberRequest:
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]
