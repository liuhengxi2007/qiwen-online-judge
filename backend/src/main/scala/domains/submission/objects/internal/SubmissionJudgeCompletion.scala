package domains.submission.objects.internal

import domains.submission.objects.SubmissionStatus
import judgeprotocol.objects.response.JudgeResult

/** worker 上报的终态判题结果；用于完成 running 状态提交的生命周期转换。 */
final case class SubmissionJudgeCompletion(
  status: SubmissionStatus,
  judgeResult: Option[JudgeResult]
)
