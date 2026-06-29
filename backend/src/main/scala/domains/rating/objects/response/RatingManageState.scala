package domains.rating.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 评分管理状态响应，当前仅包含按位置排序的评分比赛序列。 */
final case class RatingManageState(
  contests: List[RatingContestListItem]
)

/** 提供评分管理状态 JSON codec。 */
object RatingManageState:
  given Encoder[RatingManageState] = deriveEncoder[RatingManageState]
  given Decoder[RatingManageState] = deriveDecoder[RatingManageState]
