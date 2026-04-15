package domains.problemset.model

import domains.problem.model.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AddProblemToProblemSetRequest(
  problemSlug: ProblemSlug
)

object AddProblemToProblemSetRequest:
  given Encoder[AddProblemToProblemSetRequest] = deriveEncoder[AddProblemToProblemSetRequest]
  given Decoder[AddProblemToProblemSetRequest] = deriveDecoder[AddProblemToProblemSetRequest]
