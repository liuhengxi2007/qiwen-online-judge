package domains.submission.utils

import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.internal.{SubmissionJudgeCompletion, SubmissionJudgeState}
import domains.submission.objects.response.SubmissionDetail

import java.time.Instant

object SubmissionJudgeRules:

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

  def fromProtocolStatus(status: judgeprotocol.objects.SubmissionStatus): SubmissionStatus =
    status match
      case judgeprotocol.objects.SubmissionStatus.Queued => SubmissionStatus.Queued
      case judgeprotocol.objects.SubmissionStatus.Running => SubmissionStatus.Running
      case judgeprotocol.objects.SubmissionStatus.Completed => SubmissionStatus.Completed
      case judgeprotocol.objects.SubmissionStatus.Failed => SubmissionStatus.Failed

  def fromProtocolVerdict(verdict: judgeprotocol.objects.SubmissionVerdict): SubmissionVerdict =
    verdict match
      case judgeprotocol.objects.SubmissionVerdict.Accepted => SubmissionVerdict.Accepted
      case judgeprotocol.objects.SubmissionVerdict.WrongAnswer => SubmissionVerdict.WrongAnswer
      case judgeprotocol.objects.SubmissionVerdict.CompileError => SubmissionVerdict.CompileError
      case judgeprotocol.objects.SubmissionVerdict.RuntimeError => SubmissionVerdict.RuntimeError
      case judgeprotocol.objects.SubmissionVerdict.TimeLimitExceeded => SubmissionVerdict.TimeLimitExceeded
      case judgeprotocol.objects.SubmissionVerdict.SystemError => SubmissionVerdict.SystemError

  def toSubmissionLanguage(language: judgeprotocol.objects.SubmissionLanguage): Option[domains.submission.objects.SubmissionLanguage] =
    language match
      case judgeprotocol.objects.SubmissionLanguage.Cpp17 => Some(domains.submission.objects.SubmissionLanguage.Cpp17)
      case judgeprotocol.objects.SubmissionLanguage.Python3 => Some(domains.submission.objects.SubmissionLanguage.Python3)

  private def statusName(status: SubmissionStatus): String =
    status match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"
