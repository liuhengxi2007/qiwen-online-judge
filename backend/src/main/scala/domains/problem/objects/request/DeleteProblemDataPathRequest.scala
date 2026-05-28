package domains.problem.objects.request

import domains.problem.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class DeleteProblemDataPathRequest(path: ProblemDataPath)

object DeleteProblemDataPathRequest:
  given Encoder[DeleteProblemDataPathRequest] = deriveEncoder[DeleteProblemDataPathRequest]
  given Decoder[DeleteProblemDataPathRequest] = deriveDecoder[DeleteProblemDataPathRequest]
