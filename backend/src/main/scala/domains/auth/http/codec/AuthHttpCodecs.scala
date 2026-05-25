package domains.auth.http.codec

import domains.auth.model.request.*
import domains.auth.model.response.*
import domains.auth.http.codec.AuthModelHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object AuthHttpCodecs:
  export AuthModelHttpCodecs.given

  given Encoder[LoginRequest] = deriveEncoder[LoginRequest]
  given Decoder[LoginRequest] = deriveDecoder[LoginRequest]
  given Encoder[RegisterRequest] = deriveEncoder[RegisterRequest]
  given Decoder[RegisterRequest] = deriveDecoder[RegisterRequest]
  given Encoder[UpdateUserPermissionsRequest] = deriveEncoder[UpdateUserPermissionsRequest]
  given Decoder[UpdateUserPermissionsRequest] = deriveDecoder[UpdateUserPermissionsRequest]
  given Encoder[UpdateOwnAccountRequest] = deriveEncoder[UpdateOwnAccountRequest]
  given Decoder[UpdateOwnAccountRequest] = deriveDecoder[UpdateOwnAccountRequest]
  given Encoder[UpdateManagedUserAccountRequest] = deriveEncoder[UpdateManagedUserAccountRequest]
  given Decoder[UpdateManagedUserAccountRequest] = deriveDecoder[UpdateManagedUserAccountRequest]

  given Encoder[AuthAccountListItem] = deriveEncoder[AuthAccountListItem]
  given Decoder[AuthAccountListItem] = deriveDecoder[AuthAccountListItem]
  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]
  given Encoder[RegisterResponse] = deriveEncoder[RegisterResponse]
  given Decoder[RegisterResponse] = deriveDecoder[RegisterResponse]
