package domains.hack.objects.response

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建 hack 前展示的子任务信息；包含目标提交、旧最差分和策略程序要求。 */
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

/** HackSubtaskInfo 的 JSON 编解码器。 */
object HackSubtaskInfo:
  given Encoder[HackSubtaskInfo] = deriveEncoder[HackSubtaskInfo]
  given Decoder[HackSubtaskInfo] = deriveDecoder[HackSubtaskInfo]
