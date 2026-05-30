package domains.submission.objects.internal

import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.response.JudgeResult

final case class SubmissionJudgeCompletion(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  judgeResult: Option[JudgeResult]
)
