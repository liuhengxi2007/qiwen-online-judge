package domains.problemset.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 内部题单 slug 解析响应，仅表示 slug 是否存在。 */
final case class ResolveProblemSetSlugResponse(
  exists: Boolean
)

/** 提供题单 slug 解析响应 JSON codec。 */
object ResolveProblemSetSlugResponse:
  given Encoder[ResolveProblemSetSlugResponse] = deriveEncoder[ResolveProblemSetSlugResponse]
  given Decoder[ResolveProblemSetSlugResponse] = deriveDecoder[ResolveProblemSetSlugResponse]
