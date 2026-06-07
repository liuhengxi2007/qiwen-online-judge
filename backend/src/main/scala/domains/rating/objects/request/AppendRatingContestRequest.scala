package domains.rating.objects.request

import domains.contest.objects.ContestSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AppendRatingContestRequest(
  contestSlug: ContestSlug,
  m: Int
)

object AppendRatingContestRequest:
  given Encoder[AppendRatingContestRequest] = deriveEncoder[AppendRatingContestRequest]
  given Decoder[AppendRatingContestRequest] = deriveDecoder[AppendRatingContestRequest]
