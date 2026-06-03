package domains.contest.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class ContestRegistrationStatus(
  isRegistered: Boolean,
  registeredAt: Option[Instant]
)

object ContestRegistrationStatus:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  val notRegistered: ContestRegistrationStatus =
    ContestRegistrationStatus(isRegistered = false, registeredAt = None)

  def registeredAt(instant: Instant): ContestRegistrationStatus =
    ContestRegistrationStatus(isRegistered = true, registeredAt = Some(instant))

  given Encoder[ContestRegistrationStatus] = deriveEncoder[ContestRegistrationStatus]
  given Decoder[ContestRegistrationStatus] = deriveDecoder[ContestRegistrationStatus]
