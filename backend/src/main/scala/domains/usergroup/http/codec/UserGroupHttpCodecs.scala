package domains.usergroup.http.codec

import domains.user.http.codec.UserModelHttpCodecs.given
import domains.usergroup.objects.request.*
import domains.usergroup.objects.response.*
import domains.usergroup.http.codec.UserGroupModelHttpCodecs.given
import shared.http.codec.SharedHttpCodecs
import shared.http.codec.SharedHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object UserGroupHttpCodecs:
  export UserGroupModelHttpCodecs.given
  export SharedHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[CreateUserGroupRequest] = deriveEncoder[CreateUserGroupRequest]
  given Decoder[CreateUserGroupRequest] = deriveDecoder[CreateUserGroupRequest]
  given Encoder[UpdateUserGroupRequest] = deriveEncoder[UpdateUserGroupRequest]
  given Decoder[UpdateUserGroupRequest] = deriveDecoder[UpdateUserGroupRequest]
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]
  given Encoder[UpdateUserGroupMemberRoleRequest] = deriveEncoder[UpdateUserGroupMemberRoleRequest]
  given Decoder[UpdateUserGroupMemberRoleRequest] = deriveDecoder[UpdateUserGroupMemberRoleRequest]

  given Encoder[UserGroupSummary] = deriveEncoder[UserGroupSummary]
  given Decoder[UserGroupSummary] = deriveDecoder[UserGroupSummary]
  given Encoder[UserGroupDetail] = deriveEncoder[UserGroupDetail]
  given Decoder[UserGroupDetail] = deriveDecoder[UserGroupDetail]
