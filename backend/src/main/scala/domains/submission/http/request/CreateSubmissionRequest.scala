package domains.submission.http.request

import domains.submission.model.*

import domains.problem.model.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateSubmissionRequest(
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode
)

object CreateSubmissionRequest:
  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]
