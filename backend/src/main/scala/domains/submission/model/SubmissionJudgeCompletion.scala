package domains.submission.model

import judgeprotocol.model.JudgeResult

final case class SubmissionJudgeCompletion(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  judgeResult: Option[JudgeResult]
)
