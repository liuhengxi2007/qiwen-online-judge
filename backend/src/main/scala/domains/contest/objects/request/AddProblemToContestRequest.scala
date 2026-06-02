package domains.contest.objects.request

import domains.problem.objects.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AddProblemToContestRequest(
  problemSlug: ProblemSlug
)

object AddProblemToContestRequest:
  given Encoder[AddProblemToContestRequest] = deriveEncoder[AddProblemToContestRequest]
  given Decoder[AddProblemToContestRequest] = deriveDecoder[AddProblemToContestRequest]
