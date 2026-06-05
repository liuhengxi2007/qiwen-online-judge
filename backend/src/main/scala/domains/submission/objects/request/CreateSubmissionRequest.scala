package domains.submission.objects.request

import domains.submission.objects.*

import domains.problem.objects.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateSubmissionRequest(
  problemSlug: ProblemSlug,
  programs: Map[String, CreateSubmissionRequest.Program]
)

object CreateSubmissionRequest:
  final case class Program(
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  )

  object Program:
    given Encoder[Program] = deriveEncoder[Program]
    given Decoder[Program] = deriveDecoder[Program]

  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]
