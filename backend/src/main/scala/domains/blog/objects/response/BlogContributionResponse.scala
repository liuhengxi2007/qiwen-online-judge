package domains.blog.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 博客贡献值响应，用于内部聚合作者贡献。 */
final case class BlogContributionResponse(
  contribution: Int
)

/** 提供博客贡献值响应 JSON codec。 */
object BlogContributionResponse:
  given Encoder[BlogContributionResponse] = deriveEncoder[BlogContributionResponse]
  given Decoder[BlogContributionResponse] = deriveDecoder[BlogContributionResponse]
