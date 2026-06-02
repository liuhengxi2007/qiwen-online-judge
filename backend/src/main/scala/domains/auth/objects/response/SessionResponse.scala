package domains.auth.objects.response

import domains.auth.objects.EmailAddress
import domains.user.objects.UserProfileSettings

import domains.user.objects.{DisplayName, UserAvatarUrl, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SessionResponse(
  displayName: DisplayName,
  username: Username,
  avatarUrl: Option[UserAvatarUrl],
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

object SessionResponse:
  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]

  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean
  ): SessionResponse =
    SessionResponse(
      displayName = profile.displayName,
      username = profile.username,
      avatarUrl = profile.avatarUrl,
      email = email,
      preferences = profile.preferences,
      siteManager = siteManager,
      problemManager = problemManager,
      contestManager = contestManager
    )
