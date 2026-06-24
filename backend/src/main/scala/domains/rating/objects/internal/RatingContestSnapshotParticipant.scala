package domains.rating.objects.internal

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 评分比赛快照参与者，由 AppendRatingContest 从榜单生成，RatingCalculator 重放 rating_contests 快照时使用。 */
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
