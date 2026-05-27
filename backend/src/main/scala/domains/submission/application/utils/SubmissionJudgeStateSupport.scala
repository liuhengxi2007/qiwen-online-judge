package domains.submission.application.utils

import domains.submission.objects.response.SubmissionDetail
import domains.submission.objects.internal.SubmissionJudgeState

object SubmissionJudgeStateSupport:

  def fromSubmissionDetail(submission: SubmissionDetail): SubmissionJudgeState =
    SubmissionJudgeState(
      status = submission.status,
      verdict = submission.verdict,
      judgeMessage = submission.judgeMessage,
      timeUsedMs = submission.timeUsedMs,
      memoryUsedKb = submission.memoryUsedKb,
      score = submission.score,
      judgeResult = submission.judgeResult,
      startedAt = submission.startedAt,
      finishedAt = submission.finishedAt
    )
