package domains.hack.objects.response

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class HackSubtaskInfo(
  targetSubmissionId: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  targetSubmitter: UserIdentity,
  subtaskIndex: Int,
  subtaskLabel: Option[String],
  oldWorstScore: BigDecimal,
  mode: String,
  requiresStrategyProvider: Boolean
)

object HackSubtaskInfo:
  given Encoder[HackSubtaskInfo] = deriveEncoder[HackSubtaskInfo]
  given Decoder[HackSubtaskInfo] = deriveDecoder[HackSubtaskInfo]
