package domains.submission.objects.internal

import domains.submission.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult
import java.time.Instant

final case class SubmissionJudgeState(
  status: SubmissionStatus,
  judgeResult: Option[JudgeResult],
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object SubmissionJudgeState:
  val queued: SubmissionJudgeState =
    SubmissionJudgeState(
      status = SubmissionStatus.Queued,
      judgeResult = None,
      startedAt = None,
      finishedAt = None
    )
