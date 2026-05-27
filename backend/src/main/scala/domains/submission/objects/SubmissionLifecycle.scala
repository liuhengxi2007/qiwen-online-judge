package domains.submission.objects

import domains.submission.objects.internal.SubmissionJudgeCompletion

import java.time.Instant

object SubmissionLifecycle:

  def beginJudging(state: SubmissionJudgeState, startedAt: Instant): Either[String, SubmissionJudgeState] =
    state.status match
      case SubmissionStatus.Queued =>
        Right(
          state.copy(
            status = SubmissionStatus.Running,
            verdict = None,
                judgeMessage = None,
                timeUsedMs = None,
                memoryUsedKb = None,
                score = None,
                judgeResult = None,
                startedAt = Some(startedAt),
                finishedAt = None
          )
        )
      case _ =>
        Left(s"Only queued submissions can start judging, but found ${statusName(state.status)}.")

  def completeJudging(
    state: SubmissionJudgeState,
    completion: SubmissionJudgeCompletion,
    finishedAt: Instant
  ): Either[String, SubmissionJudgeState] =
    state.status match
      case SubmissionStatus.Running =>
        completion.status match
          case SubmissionStatus.Completed | SubmissionStatus.Failed =>
            Right(
              state.copy(
                status = completion.status,
                verdict = completion.verdict,
                judgeMessage = completion.judgeMessage,
                timeUsedMs = completion.timeUsedMs,
                memoryUsedKb = completion.memoryUsedKb,
                score = completion.score,
                judgeResult = completion.judgeResult,
                finishedAt = Some(finishedAt)
              )
            )
          case _ =>
            Left("Judging may only finish with completed or failed status.")
      case _ =>
        Left(s"Only running submissions can finish judging, but found ${statusName(state.status)}.")

  private def statusName(status: SubmissionStatus): String =
    status match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"
