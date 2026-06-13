package domains.rating.objects.request

import domains.contest.objects.ContestSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 追加评分比赛请求体，指定比赛 slug 和本场替换粒子数量 m。 */
final case class AppendRatingContestRequest(
  contestSlug: ContestSlug,
  m: Int
)

/** 提供追加评分比赛请求体 JSON codec。 */
object AppendRatingContestRequest:
  given Encoder[AppendRatingContestRequest] = deriveEncoder[AppendRatingContestRequest]
  given Decoder[AppendRatingContestRequest] = deriveDecoder[AppendRatingContestRequest]
