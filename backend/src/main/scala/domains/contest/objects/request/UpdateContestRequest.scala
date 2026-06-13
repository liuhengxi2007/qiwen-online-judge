package domains.contest.objects.request

import domains.contest.objects.{ContestDescription, ContestTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

/** 更新比赛请求体，保留 slug 不变，只更新内容、时间和访问策略。 */
final case class UpdateContestRequest(
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  accessPolicy: ResourceAccessPolicy
)

/** 提供更新比赛请求体 JSON codec。 */
object UpdateContestRequest:
  given Encoder[UpdateContestRequest] = deriveEncoder[UpdateContestRequest]
  given Decoder[UpdateContestRequest] = deriveDecoder[UpdateContestRequest]
