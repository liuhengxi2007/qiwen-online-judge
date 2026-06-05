package domains.contest.objects.request

import domains.contest.objects.{ContestDescription, ContestTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

final case class UpdateContestRequest(
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  accessPolicy: ResourceAccessPolicy
)

object UpdateContestRequest:
  given Encoder[UpdateContestRequest] = deriveEncoder[UpdateContestRequest]
  given Decoder[UpdateContestRequest] = deriveDecoder[UpdateContestRequest]
