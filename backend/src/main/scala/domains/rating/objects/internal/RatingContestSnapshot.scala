package domains.rating.objects.internal

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 评分比赛快照，持久化追加比赛时的参与者排序数据。 */
final case class RatingContestSnapshot(
  participants: List[RatingContestSnapshotParticipant]
)

/** 提供评分比赛快照 JSON codec，用于 rating_contests 表中的快照字段。 */
object RatingContestSnapshot:
  given Encoder[RatingContestSnapshot] = deriveEncoder[RatingContestSnapshot]
  given Decoder[RatingContestSnapshot] = deriveDecoder[RatingContestSnapshot]
