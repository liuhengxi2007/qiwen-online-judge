package domains.rating.objects.internal

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 评分比赛快照中的单个参与者，保留用户名、排名、总分和罚时。 */
final case class RatingContestSnapshotParticipant(
  username: Username,
  rank: Int,
  totalScore: BigDecimal,
  penaltyMillis: Long
)

/** 提供评分参与者快照 JSON codec。 */
object RatingContestSnapshotParticipant:
  given Encoder[RatingContestSnapshotParticipant] = deriveEncoder[RatingContestSnapshotParticipant]
  given Decoder[RatingContestSnapshotParticipant] = deriveDecoder[RatingContestSnapshotParticipant]
