package domains.submission.application



import cats.effect.IO
import domains.submission.application.utils.SubmissionJudgeStateSupport
import domains.submission.objects.{SubmissionId, SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeCompletion, SubmissionJudgeState}
import domains.submission.table.submission.{SubmissionJudgeTable, SubmissionQueryTable}
import judgeprotocol.objects.{JudgerId, ReportJudgeResultRequest}

import java.sql.Connection
import java.time.Instant

object SubmissionCommands:
  export SubmissionCommandResults.*
  export SubmissionMutationCommands.*
  export SubmissionQueryCommands.*

  enum ClaimNextJudgeTaskResult:
    case ValidationFailed(message: String)
    case NoTask
    case Claimed(submission: ClaimedSubmission)

  enum RecordJudgeResult:
    case ValidationFailed(message: String)
    case SubmissionNotFound
    case Updated

  def claimNextJudgeTask(
    connection: Connection,
    supportedLanguages: List[judgeprotocol.objects.SubmissionLanguage],
    claimedAt: Instant
  ): IO[ClaimNextJudgeTaskResult] =
    SubmissionLifecycle.beginJudging(SubmissionJudgeState.queued, claimedAt) match
      case Left(message) =>
        IO.pure(ClaimNextJudgeTaskResult.ValidationFailed(message))
      case Right(runningState) =>
        SubmissionJudgeTable.claimNextForLanguages(connection, supportedLanguages.flatMap(toSubmissionLanguage), runningState).map {
          case None => ClaimNextJudgeTaskResult.NoTask
          case Some(claimedSubmission) => ClaimNextJudgeTaskResult.Claimed(claimedSubmission)
        }

  def failClaimedJudgeTask(
    connection: Connection,
    claimedSubmission: ClaimedSubmission,
    judgerId: JudgerId,
    claimedAt: Instant,
    message: String
  ): IO[Either[String, Unit]] =
    SubmissionLifecycle.beginJudging(SubmissionJudgeState.queued, claimedAt).flatMap { runningState =>
      SubmissionLifecycle.completeJudging(
        runningState,
        SubmissionJudgeCompletion(
          status = SubmissionStatus.Failed,
          verdict = Some(SubmissionVerdict.SystemError),
          judgeMessage = Some(s"${judgerId.value}: $message"),
          timeUsedMs = None,
          memoryUsedKb = None,
          score = None,
          judgeResult = None
        ),
        claimedAt
      )
    } match
      case Left(lifecycleMessage) =>
        IO.pure(Left(lifecycleMessage))
      case Right(failedState) =>
        SubmissionJudgeTable.updateJudgeState(connection, claimedSubmission.id, failedState).as(Right(()))

  def recordJudgeResult(
    connection: Connection,
    submissionId: SubmissionId,
    request: ReportJudgeResultRequest,
    completedAt: Instant
  ): IO[RecordJudgeResult] =
    request.status match
      case judgeprotocol.objects.SubmissionStatus.Completed | judgeprotocol.objects.SubmissionStatus.Failed =>
        SubmissionQueryTable.findById(connection, submissionId).flatMap {
          case None =>
            IO.pure(RecordJudgeResult.SubmissionNotFound)
          case Some(submission) =>
            SubmissionLifecycle
              .completeJudging(
                SubmissionJudgeStateSupport.fromSubmissionDetail(submission),
                SubmissionJudgeCompletion(
                  status = fromProtocolStatus(request.status),
                  verdict = request.verdict.map(fromProtocolVerdict),
                  judgeMessage = request.judgeMessage,
                  timeUsedMs = request.timeUsedMs,
                  memoryUsedKb = request.memoryUsedKb,
                  score = request.score,
                  judgeResult = request.judgeResult
                ),
                completedAt
              )
              .fold(
                message => IO.pure(RecordJudgeResult.ValidationFailed(message)),
                completedState =>
                  SubmissionJudgeTable
                    .updateJudgeState(connection, submissionId, completedState)
                    .as(RecordJudgeResult.Updated)
              )
        }
      case _ =>
        IO.pure(RecordJudgeResult.ValidationFailed("Judge results may only set status to completed or failed."))

  private def fromProtocolStatus(status: judgeprotocol.objects.SubmissionStatus): SubmissionStatus =
    status match
      case judgeprotocol.objects.SubmissionStatus.Queued => SubmissionStatus.Queued
      case judgeprotocol.objects.SubmissionStatus.Running => SubmissionStatus.Running
      case judgeprotocol.objects.SubmissionStatus.Completed => SubmissionStatus.Completed
      case judgeprotocol.objects.SubmissionStatus.Failed => SubmissionStatus.Failed

  private def fromProtocolVerdict(verdict: judgeprotocol.objects.SubmissionVerdict): SubmissionVerdict =
    verdict match
      case judgeprotocol.objects.SubmissionVerdict.Accepted => SubmissionVerdict.Accepted
      case judgeprotocol.objects.SubmissionVerdict.WrongAnswer => SubmissionVerdict.WrongAnswer
      case judgeprotocol.objects.SubmissionVerdict.CompileError => SubmissionVerdict.CompileError
      case judgeprotocol.objects.SubmissionVerdict.RuntimeError => SubmissionVerdict.RuntimeError
      case judgeprotocol.objects.SubmissionVerdict.TimeLimitExceeded => SubmissionVerdict.TimeLimitExceeded
      case judgeprotocol.objects.SubmissionVerdict.SystemError => SubmissionVerdict.SystemError

  private def toSubmissionLanguage(language: judgeprotocol.objects.SubmissionLanguage): Option[domains.submission.objects.SubmissionLanguage] =
    language match
      case judgeprotocol.objects.SubmissionLanguage.Cpp17 => Some(domains.submission.objects.SubmissionLanguage.Cpp17)
      case judgeprotocol.objects.SubmissionLanguage.Python3 => Some(domains.submission.objects.SubmissionLanguage.Python3)
