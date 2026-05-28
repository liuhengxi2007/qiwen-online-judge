package domains.auth.objects.response

import domains.auth.objects.*

import domains.user.objects.{DisplayName, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SessionResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean
)

object SessionResponse:
  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]

  def fromAuthUser(user: AuthUser): SessionResponse =
    SessionResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = UserPreferences(
        displayMode = user.displayMode,
        locale = user.locale,
        problemTitleDisplayMode = user.problemTitleDisplayMode,
        autoMarkMessageRead = user.autoMarkMessageRead
      ),
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )
