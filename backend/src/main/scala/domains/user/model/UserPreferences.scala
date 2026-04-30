package domains.user.model

import domains.problem.model.ProblemTitleDisplayMode
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserPreferences(
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean
)

object UserPreferences:
  given Encoder[UserPreferences] = deriveEncoder[UserPreferences]
  given Decoder[UserPreferences] = deriveDecoder[UserPreferences]
