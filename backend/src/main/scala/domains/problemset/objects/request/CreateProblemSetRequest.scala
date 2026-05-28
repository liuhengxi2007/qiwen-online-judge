package domains.problemset.objects.request

import domains.problemset.objects.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

final case class CreateProblemSetRequest(
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)

object CreateProblemSetRequest:
  given Encoder[CreateProblemSetRequest] = deriveEncoder[CreateProblemSetRequest]
  given Decoder[CreateProblemSetRequest] = deriveDecoder[CreateProblemSetRequest]
