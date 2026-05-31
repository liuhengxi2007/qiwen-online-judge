package domains.usergroup.objects.request

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AddUserGroupMemberRequest(
  username: Username,
  role: NewUserGroupMemberRole
)

object AddUserGroupMemberRequest:
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]
