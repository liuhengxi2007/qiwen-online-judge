package domains.contest.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ContestRegistrationStatus(
  isRegistered: Boolean
)

object ContestRegistrationStatus:
  val notRegistered: ContestRegistrationStatus =
    ContestRegistrationStatus(isRegistered = false)

  val registered: ContestRegistrationStatus =
    ContestRegistrationStatus(isRegistered = true)

  given Encoder[ContestRegistrationStatus] = deriveEncoder[ContestRegistrationStatus]
  given Decoder[ContestRegistrationStatus] = deriveDecoder[ContestRegistrationStatus]
