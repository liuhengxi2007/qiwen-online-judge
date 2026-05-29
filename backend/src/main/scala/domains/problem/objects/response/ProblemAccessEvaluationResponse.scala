package domains.problem.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemAccessEvaluationResponse(
  problem: Option[ProblemDetail],
  canView: Boolean,
  canManage: Boolean
)

object ProblemAccessEvaluationResponse:
  given Encoder[ProblemAccessEvaluationResponse] = deriveEncoder[ProblemAccessEvaluationResponse]
  given Decoder[ProblemAccessEvaluationResponse] = deriveDecoder[ProblemAccessEvaluationResponse]
