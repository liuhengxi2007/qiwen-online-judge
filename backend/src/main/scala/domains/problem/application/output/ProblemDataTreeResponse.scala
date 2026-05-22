package domains.problem.application.output

import domains.problem.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemDataTreeResponse(items: List[ProblemDataTreeNode])

object ProblemDataTreeResponse:
  given Encoder[ProblemDataTreeResponse] = deriveEncoder[ProblemDataTreeResponse]
  given Decoder[ProblemDataTreeResponse] = deriveDecoder[ProblemDataTreeResponse]
