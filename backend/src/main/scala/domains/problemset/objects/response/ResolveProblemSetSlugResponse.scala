package domains.problemset.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ResolveProblemSetSlugResponse(
  exists: Boolean
)

object ResolveProblemSetSlugResponse:
  given Encoder[ResolveProblemSetSlugResponse] = deriveEncoder[ResolveProblemSetSlugResponse]
  given Decoder[ResolveProblemSetSlugResponse] = deriveDecoder[ResolveProblemSetSlugResponse]
