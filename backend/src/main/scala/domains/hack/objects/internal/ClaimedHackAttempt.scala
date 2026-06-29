package domains.hack.objects.internal

import domains.hack.objects.HackId
import domains.submission.objects.internal.ClaimedSubmission
import domains.user.objects.Username
import judgeprotocol.objects.response.JudgeResult

/** hack 领取任务载荷，由 ClaimNextHackAttempt 内部 API 返回，ClaimJudgeTask 用它组装 judge-protocol 的 HackTask。 */
final case class ClaimedHackAttempt(
  hackId: HackId,
  targetSubmission: ClaimedSubmission,
  authorUsername: Username,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String],
  oldResult: JudgeResult
)
