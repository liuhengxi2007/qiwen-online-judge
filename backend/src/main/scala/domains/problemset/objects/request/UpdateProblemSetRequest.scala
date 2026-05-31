package domains.problemset.objects.request

import domains.problemset.objects.*
import domains.user.objects.Username

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

final case class UpdateProblemSetRequest(
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy,
  authorUsername: Option[Username]
)

object UpdateProblemSetRequest:
  given Encoder[UpdateProblemSetRequest] = deriveEncoder[UpdateProblemSetRequest]
  given Decoder[UpdateProblemSetRequest] = deriveDecoder[UpdateProblemSetRequest]
