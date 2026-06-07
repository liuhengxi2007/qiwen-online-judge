package domains.rating.objects.internal

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RatingContestSnapshot(
  participants: List[RatingContestSnapshotParticipant]
)

object RatingContestSnapshot:
  given Encoder[RatingContestSnapshot] = deriveEncoder[RatingContestSnapshot]
  given Decoder[RatingContestSnapshot] = deriveDecoder[RatingContestSnapshot]
