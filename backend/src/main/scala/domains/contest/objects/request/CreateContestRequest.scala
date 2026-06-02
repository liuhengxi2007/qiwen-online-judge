package domains.contest.objects.request

import domains.contest.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

final case class CreateContestRequest(
  slug: ContestSlug,
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  accessPolicy: ResourceAccessPolicy
)

object CreateContestRequest:
  given Encoder[CreateContestRequest] = deriveEncoder[CreateContestRequest]
  given Decoder[CreateContestRequest] = deriveDecoder[CreateContestRequest]
