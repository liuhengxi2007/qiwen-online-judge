package domains.submission.utils

import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.internal.{SubmissionJudgeCompletion, SubmissionJudgeState}
import domains.submission.objects.internal.SubmissionDetailRecord
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult}

import java.time.Instant

object SubmissionJudgeRules:

  def beginJudging(state: SubmissionJudgeState, startedAt: Instant): Either[String, SubmissionJudgeState] =
    state.status match
      case SubmissionStatus.Queued =>
        Right(
          state.copy(
            status = SubmissionStatus.Running,
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
            for
              judgeResult <- completion.judgeResult.toRight("Terminal judge updates must include judgeResult.")
              _ <- validateReasonMatchesVerdict(judgeResult)
              _ <- validateStatusMatchesResult(completion.status, judgeResult)
            yield
              state.copy(
                status = completion.status,
                judgeResult = Some(judgeResult),
                finishedAt = Some(finishedAt)
              )
          case _ =>
            Left("Judging may only finish with completed or failed status.")
      case _ =>
        Left(s"Only running submissions can finish judging, but found ${statusName(state.status)}.")

  def fromSubmissionRecord(submission: SubmissionDetailRecord): SubmissionJudgeState =
    SubmissionJudgeState(
      status = submission.status,
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

  private def validateStatusMatchesResult(status: SubmissionStatus, judgeResult: JudgeResult): Either[String, Unit] =
    val hasSystemError = containsSystemError(judgeResult)
    val hasReason = containsFailureReason(judgeResult)
    status match
      case SubmissionStatus.Completed if hasSystemError =>
        Left("Completed judge updates must not include a system error judgeResult.")
      case SubmissionStatus.Completed if hasReason =>
        Left("Completed judge updates must not include judge failure reasons.")
      case SubmissionStatus.Failed if !hasSystemError =>
        Left("Failed judge updates must include a system error judgeResult.")
      case SubmissionStatus.Completed | SubmissionStatus.Failed =>
        Right(())
      case _ =>
        Left("Judging may only finish with completed or failed status.")

  private def validateReasonMatchesVerdict(judgeResult: JudgeResult): Either[String, Unit] =
    validateNodeReason("judgeResult", judgeResult.verdict, judgeResult.reason)
      .flatMap(_ =>
        judgeResult.subtasks
          .map(subtask =>
            validateNodeReason(s"subtask ${subtask.name}", subtask.verdict, subtask.reason)
              .flatMap(_ =>
                subtask.testcases
                  .map(testcase => validateNodeReason(s"testcase ${testcase.name}", testcase.verdict, testcase.reason))
                  .collectFirst { case Left(message) => Left(message) }
                  .getOrElse(Right(()))
              )
          )
          .collectFirst { case Left(message) => Left(message) }
          .getOrElse(Right(()))
      )

  private def validateNodeReason(
    label: String,
    verdict: judgeprotocol.objects.SubmissionVerdict,
    reason: Option[JudgeFailureReason]
  ): Either[String, Unit] =
    if reason.nonEmpty && verdict != judgeprotocol.objects.SubmissionVerdict.SystemError then
      Left(s"$label reason is only allowed with system_error verdict.")
    else if verdict == judgeprotocol.objects.SubmissionVerdict.SystemError && reason.isEmpty then
      Left(s"$label system_error verdict must include reason.")
    else
      Right(())

  private def containsSystemError(judgeResult: JudgeResult): Boolean =
    judgeResult.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError ||
      judgeResult.subtasks.exists(subtask =>
        subtask.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError ||
          subtask.testcases.exists(_.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError)
      )

  private def containsFailureReason(judgeResult: JudgeResult): Boolean =
    judgeResult.reason.nonEmpty ||
      judgeResult.subtasks.exists(subtask =>
        subtask.reason.nonEmpty ||
          subtask.testcases.exists(_.reason.nonEmpty)
      )
