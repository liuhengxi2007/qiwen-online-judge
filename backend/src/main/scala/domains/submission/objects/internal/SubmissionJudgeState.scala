package domains.submission.objects.internal

import domains.submission.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult
import java.time.Instant

/** 提交判题状态快照；保存状态、结果以及开始/结束时间。 */
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
