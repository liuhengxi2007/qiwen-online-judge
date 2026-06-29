package domains.problem.objects.response

import domains.problem.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 题目搜索建议响应；只返回可展示的 slug 和标题。 */
final case class ProblemSuggestion(
  slug: ProblemSlug,
  title: ProblemTitle
)

/** ProblemSuggestion 的 JSON 编解码器。 */
object ProblemSuggestion:
  given Encoder[ProblemSuggestion] = deriveEncoder[ProblemSuggestion]
  given Decoder[ProblemSuggestion] = deriveDecoder[ProblemSuggestion]
