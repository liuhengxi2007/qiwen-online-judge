package domains.hack.objects.internal

import domains.hack.objects.HackId
import domains.submission.objects.internal.ClaimedSubmission
import domains.user.objects.Username
import judgeprotocol.objects.response.JudgeResult

/** 已领取的 hack attempt；携带目标提交任务、hack 输入和原始判题结果，供 worker 构建 HackTask。 */
final case class ClaimedHackAttempt(
  hackId: HackId,
  targetSubmission: ClaimedSubmission,
  authorUsername: Username,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String],
  oldResult: JudgeResult
)
