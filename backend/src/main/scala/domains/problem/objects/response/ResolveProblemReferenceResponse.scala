package domains.problem.objects.response

import domains.problem.objects.ProblemReference
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 内部题目引用解析响应；problem 为空表示 slug 当前没有对应题目。 */
final case class ResolveProblemReferenceResponse(
  problem: Option[ProblemReference]
)

/** ResolveProblemReferenceResponse 的 JSON 编解码器。 */
object ResolveProblemReferenceResponse:
  given Encoder[ResolveProblemReferenceResponse] = deriveEncoder[ResolveProblemReferenceResponse]
  given Decoder[ResolveProblemReferenceResponse] = deriveDecoder[ResolveProblemReferenceResponse]
