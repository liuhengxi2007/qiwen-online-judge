package domains.submission.model

import java.time.Instant

final case class SubmissionJudgeState(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object SubmissionJudgeState:
  val queued: SubmissionJudgeState =
    SubmissionJudgeState(
      status = SubmissionStatus.Queued,
      verdict = None,
      judgeMessage = None,
      startedAt = None,
      finishedAt = None
    )

  def fromSubmissionDetail(submission: SubmissionDetail): SubmissionJudgeState =
    SubmissionJudgeState(
      status = submission.status,
      verdict = submission.verdict,
      judgeMessage = submission.judgeMessage,
      startedAt = submission.startedAt,
      finishedAt = submission.finishedAt
    )

final case class SubmissionJudgeCompletion(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String]
)

object SubmissionLifecycle:

  def beginJudging(state: SubmissionJudgeState, startedAt: Instant): Either[String, SubmissionJudgeState] =
    state.status match
      case SubmissionStatus.Queued =>
        Right(
          state.copy(
            status = SubmissionStatus.Running,
            verdict = None,
            judgeMessage = None,
            startedAt = Some(startedAt),
            finishedAt = None
          )
        )
      case _ =>
        Left(s"Only queued submissions can start judging, but found ${SubmissionStatus.toDatabase(state.status)}.")

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
                finishedAt = Some(finishedAt)
              )
            )
          case _ =>
            Left("Judging may only finish with completed or failed status.")
      case _ =>
        Left(s"Only running submissions can finish judging, but found ${SubmissionStatus.toDatabase(state.status)}.")
