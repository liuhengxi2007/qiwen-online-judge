package domains.problem.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 跨域使用的题目轻量引用；只暴露稳定 id、slug 和标题。 */
final case class ProblemReference(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle
)

/** ProblemReference 的 JSON 编解码器。 */
object ProblemReference:
  given Encoder[ProblemReference] = deriveEncoder[ProblemReference]
  given Decoder[ProblemReference] = deriveDecoder[ProblemReference]
