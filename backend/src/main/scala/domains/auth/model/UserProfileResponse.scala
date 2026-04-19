package domains.auth.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserProfileResponse(
  username: Username,
  displayName: DisplayName,
  contribution: UserContribution,
  acceptedProblems: List[UserAcceptedProblem]
)

object UserProfileResponse:
  given Encoder[UserProfileResponse] = deriveEncoder[UserProfileResponse]
  given Decoder[UserProfileResponse] = deriveDecoder[UserProfileResponse]
