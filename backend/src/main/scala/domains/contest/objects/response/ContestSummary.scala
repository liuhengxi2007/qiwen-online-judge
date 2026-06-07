package domains.contest.objects.response

import domains.contest.objects.*
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

final case class ContestSummary(
  id: ContestId,
  slug: ContestSlug,
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  accessPolicy: ResourceAccessPolicy,
  registrationStatus: ContestRegistrationStatus,
  canViewDetail: Boolean,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

object ContestSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ContestSummary] = deriveEncoder[ContestSummary]
  given Decoder[ContestSummary] = deriveDecoder[ContestSummary]
