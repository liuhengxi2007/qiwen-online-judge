package domains.problem.objects.request

import domains.problem.objects.*
import domains.user.objects.Username

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

final case class UpdateProblemRequest(
  title: ProblemTitle,
  statement: ProblemStatementText,
  accessPolicy: ResourceAccessPolicy,
  otherUserSubmissionAccess: OtherUserSubmissionAccess,
  authorUsername: Option[Username]
)

object UpdateProblemRequest:
  given Encoder[UpdateProblemRequest] = deriveEncoder[UpdateProblemRequest]
  given Decoder[UpdateProblemRequest] = deriveDecoder[UpdateProblemRequest]
