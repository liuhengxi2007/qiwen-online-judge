package domains.user.http.codec

import domains.auth.http.codec.AuthModelHttpCodecs.given
import domains.auth.application.output.SessionResponse
import domains.user.application.input.*
import domains.user.application.output.*
import shared.model.PageRequest
import shared.http.codec.SharedHttpCodecs
import shared.http.codec.SharedHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs.given
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

object UserHttpCodecs:
  export UserModelHttpCodecs.given
  export SharedHttpCodecs.given

  given Encoder[UserSearchQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserSearchQuery] = Decoder.decodeString.emap(UserSearchQuery.parse)

  given Encoder[UserListRequest] = Encoder.instance(request =>
    Json.obj(
      "query" -> request.query.asJson,
      "page" -> request.pageRequest.page.asJson,
      "pageSize" -> request.pageRequest.pageSize.asJson
    )
  )

  given Decoder[UserListRequest] = Decoder.instance { cursor =>
    for
      query <- cursor.downField("query").as[Option[UserSearchQuery]]
      page <- cursor.downField("page").as[Int]
      pageSize <- cursor.downField("pageSize").as[Int]
    yield UserListRequest(query = query, pageRequest = PageRequest(page = page, pageSize = pageSize))
  }

  given Encoder[UpdateUserPermissionsRequest] = deriveEncoder[UpdateUserPermissionsRequest]
  given Decoder[UpdateUserPermissionsRequest] = deriveDecoder[UpdateUserPermissionsRequest]
  given Encoder[UpdateOwnProfileRequest] = deriveEncoder[UpdateOwnProfileRequest]
  given Decoder[UpdateOwnProfileRequest] = deriveDecoder[UpdateOwnProfileRequest]
  given Encoder[UpdateOwnPreferencesRequest] = deriveEncoder[UpdateOwnPreferencesRequest]
  given Decoder[UpdateOwnPreferencesRequest] = deriveDecoder[UpdateOwnPreferencesRequest]
  given Encoder[UpdateOwnAccountRequest] = deriveEncoder[UpdateOwnAccountRequest]
  given Decoder[UpdateOwnAccountRequest] = deriveDecoder[UpdateOwnAccountRequest]
  given Encoder[UpdateManagedUserProfileRequest] = deriveEncoder[UpdateManagedUserProfileRequest]
  given Decoder[UpdateManagedUserProfileRequest] = deriveDecoder[UpdateManagedUserProfileRequest]
  given Encoder[UpdateManagedUserPreferencesRequest] = deriveEncoder[UpdateManagedUserPreferencesRequest]
  given Decoder[UpdateManagedUserPreferencesRequest] = deriveDecoder[UpdateManagedUserPreferencesRequest]
  given Encoder[UpdateManagedUserAccountRequest] = deriveEncoder[UpdateManagedUserAccountRequest]
  given Decoder[UpdateManagedUserAccountRequest] = deriveDecoder[UpdateManagedUserAccountRequest]

  given Encoder[AuthUserListItem] = deriveEncoder[AuthUserListItem]
  given Decoder[AuthUserListItem] = deriveDecoder[AuthUserListItem]
  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]
  given Encoder[UserProfileResponse] = deriveEncoder[UserProfileResponse]
  given Decoder[UserProfileResponse] = deriveDecoder[UserProfileResponse]
  given Encoder[UserRanklistItem] = deriveEncoder[UserRanklistItem]
  given Decoder[UserRanklistItem] = deriveDecoder[UserRanklistItem]
  given Encoder[UserAcceptedRanklistItem] = deriveEncoder[UserAcceptedRanklistItem]
  given Decoder[UserAcceptedRanklistItem] = deriveDecoder[UserAcceptedRanklistItem]
