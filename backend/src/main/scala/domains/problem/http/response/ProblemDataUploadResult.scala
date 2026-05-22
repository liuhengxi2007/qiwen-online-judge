package domains.problem.http.response

import domains.problem.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemDataUploadResult(
  problem: ProblemDetail,
  uploadedFileCount: Int
)

object ProblemDataUploadResult:
  given Encoder[ProblemDataUploadResult] = deriveEncoder[ProblemDataUploadResult]
  given Decoder[ProblemDataUploadResult] = deriveDecoder[ProblemDataUploadResult]
