package domains.user.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserListRequest(
  query: Option[String],
  page: Int,
  pageSize: Int
)

object UserListRequest:
  given Encoder[UserListRequest] = deriveEncoder[UserListRequest]
  given Decoder[UserListRequest] = deriveDecoder[UserListRequest]
