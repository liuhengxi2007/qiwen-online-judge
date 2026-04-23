package domains.user.model

import domains.auth.model.{UserDisplayMode, UserLocale}
import domains.problem.model.ProblemTitleDisplayMode
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserPreferences(
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode
)

object UserPreferences:
  given Encoder[UserPreferences] = deriveEncoder[UserPreferences]
  given Decoder[UserPreferences] = deriveDecoder[UserPreferences]
