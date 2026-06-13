package domains.problem.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 内部题目访问评估结果；problem 为空表示题目不存在或调用方需自行隐藏。 */
final case class ProblemAccessEvaluationResponse(
  problem: Option[ProblemDetail],
  canView: Boolean,
  canManage: Boolean
)

/** ProblemAccessEvaluationResponse 的 JSON 编解码器。 */
object ProblemAccessEvaluationResponse:
  given Encoder[ProblemAccessEvaluationResponse] = deriveEncoder[ProblemAccessEvaluationResponse]
  given Decoder[ProblemAccessEvaluationResponse] = deriveDecoder[ProblemAccessEvaluationResponse]
