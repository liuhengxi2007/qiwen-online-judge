package domains.problem.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemListRequest(
  query: Option[String],
  page: Int,
  pageSize: Int
)

object ProblemListRequest:
  given Encoder[ProblemListRequest] = deriveEncoder[ProblemListRequest]
  given Decoder[ProblemListRequest] = deriveDecoder[ProblemListRequest]
