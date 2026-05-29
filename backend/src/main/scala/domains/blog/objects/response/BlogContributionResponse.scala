package domains.blog.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class BlogContributionResponse(
  contribution: Int
)

object BlogContributionResponse:
  given Encoder[BlogContributionResponse] = deriveEncoder[BlogContributionResponse]
  given Decoder[BlogContributionResponse] = deriveDecoder[BlogContributionResponse]
