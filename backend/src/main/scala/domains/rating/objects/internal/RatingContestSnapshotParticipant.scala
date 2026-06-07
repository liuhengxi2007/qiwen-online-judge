package domains.rating.objects.internal

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RatingContestSnapshotParticipant(
  username: Username,
  rank: Int,
  totalScore: BigDecimal,
  penaltyMillis: Long
)

object RatingContestSnapshotParticipant:
  given Encoder[RatingContestSnapshotParticipant] = deriveEncoder[RatingContestSnapshotParticipant]
  given Decoder[RatingContestSnapshotParticipant] = deriveDecoder[RatingContestSnapshotParticipant]
