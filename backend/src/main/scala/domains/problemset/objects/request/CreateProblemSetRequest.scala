package domains.problemset.objects.request

import domains.problemset.objects.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

/** 创建题单请求体，包含基础信息和访问策略。 */
final case class CreateProblemSetRequest(
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)

/** 提供创建题单请求体 JSON codec。 */
object CreateProblemSetRequest:
  given Encoder[CreateProblemSetRequest] = deriveEncoder[CreateProblemSetRequest]
  given Decoder[CreateProblemSetRequest] = deriveDecoder[CreateProblemSetRequest]
