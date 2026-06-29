package domains.submission.objects.internal

import domains.submission.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult
import java.time.Instant

/** 提交判题状态快照，作为 ClaimNext/Get/UpdateSubmissionJudgeState 内部 API 载荷，并由 SubmissionJudgeRules 校验流转。 */
final case class SubmissionJudgeState(
  status: SubmissionStatus,
  judgeResult: Option[JudgeResult],
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

/** 提交判题状态的便捷构造。 */
object SubmissionJudgeState:
  val queued: SubmissionJudgeState =
    SubmissionJudgeState(
      status = SubmissionStatus.Queued,
      judgeResult = None,
      startedAt = None,
      finishedAt = None
    )
