package domains.problem.http.response

import domains.problem.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemDataFileListResponse(items: List[ProblemDataFilename])

object ProblemDataFileListResponse:
  given Encoder[ProblemDataFileListResponse] = deriveEncoder[ProblemDataFileListResponse]
  given Decoder[ProblemDataFileListResponse] = deriveDecoder[ProblemDataFileListResponse]
