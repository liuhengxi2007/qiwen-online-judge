package domains.submission.objects.internal

import domains.submission.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult

final case class SubmissionJudgeCompletion(
  status: SubmissionStatus,
  judgeResult: Option[JudgeResult]
)
