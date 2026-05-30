package domains.submission.objects.internal

import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.response.JudgeResult
import java.time.Instant

final case class SubmissionJudgeState(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  judgeResult: Option[JudgeResult],
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object SubmissionJudgeState:
  val queued: SubmissionJudgeState =
    SubmissionJudgeState(
      status = SubmissionStatus.Queued,
      verdict = None,
      judgeMessage = None,
      timeUsedMs = None,
      memoryUsedKb = None,
      score = None,
      judgeResult = None,
      startedAt = None,
      finishedAt = None
    )
