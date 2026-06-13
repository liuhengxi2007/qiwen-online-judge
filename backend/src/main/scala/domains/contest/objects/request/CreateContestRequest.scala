package domains.contest.objects.request

import domains.contest.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

/** 创建比赛请求体，包含基础信息、时间范围和访问策略。 */
final case class CreateContestRequest(
  slug: ContestSlug,
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  accessPolicy: ResourceAccessPolicy
)

/** 提供创建比赛请求体 JSON codec。 */
object CreateContestRequest:
  given Encoder[CreateContestRequest] = deriveEncoder[CreateContestRequest]
  given Decoder[CreateContestRequest] = deriveDecoder[CreateContestRequest]
