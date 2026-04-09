package domains.judge.application

import cats.effect.IO
import database.DatabaseSession
import domains.submission.model.{SubmissionId, SubmissionStatus, SubmissionVerdict}
import domains.submission.table.SubmissionTable
import judgeprotocol.model.{JudgeTask, JudgerName, ReportJudgeResultRequest}

object JudgeCommands:

  enum ClaimJudgeTaskResult:
    case NoTask
    case ValidationFailed(message: String)
    case Claimed(task: JudgeTask)

  enum ReportJudgeResult:
    case ValidationFailed(message: String)
    case SubmissionNotFound
    case Updated

  def claimCpp17Task(
    databaseSession: DatabaseSession,
    judgerName: JudgerName
  ): IO[ClaimJudgeTaskResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionTable.claimNextCpp17(connection).flatMap {
        case None =>
          IO.pure(ClaimJudgeTaskResult.NoTask)
        case Some(claimedSubmission) =>
          JudgeTaskBuilder.buildJudgeTask(connection, claimedSubmission).flatMap {
            case Left(message) =>
              SubmissionTable
                .markCompleted(
                  connection,
                  claimedSubmission.id,
                  status = SubmissionStatus.Failed,
                  verdict = Some(SubmissionVerdict.SystemError),
                  judgeMessage = Some(s"${judgerName.value}: $message")
                )
                .as(ClaimJudgeTaskResult.ValidationFailed(message))
            case Right(task) =>
              IO.pure(ClaimJudgeTaskResult.Claimed(task))
          }
      }
    }

  def reportJudgeResult(
    databaseSession: DatabaseSession,
    submissionId: SubmissionId,
    request: ReportJudgeResultRequest
  ): IO[ReportJudgeResult] =
    request.status match
      case judgeprotocol.model.SubmissionStatus.Completed | judgeprotocol.model.SubmissionStatus.Failed =>
        databaseSession.withTransactionConnection { connection =>
          SubmissionTable.findById(connection, submissionId).flatMap {
            case None =>
              IO.pure(ReportJudgeResult.SubmissionNotFound)
            case Some(_) =>
              SubmissionTable
                .markCompleted(
                  connection,
                  submissionId,
                  fromProtocolStatus(request.status),
                  request.verdict.map(fromProtocolVerdict),
                  request.judgeMessage
                )
                .as(ReportJudgeResult.Updated)
          }
        }
      case _ =>
        IO.pure(ReportJudgeResult.ValidationFailed("Judge results may only set status to completed or failed."))

  private def fromProtocolStatus(status: judgeprotocol.model.SubmissionStatus): SubmissionStatus =
    status match
      case judgeprotocol.model.SubmissionStatus.Queued => SubmissionStatus.Queued
      case judgeprotocol.model.SubmissionStatus.Running => SubmissionStatus.Running
      case judgeprotocol.model.SubmissionStatus.Completed => SubmissionStatus.Completed
      case judgeprotocol.model.SubmissionStatus.Failed => SubmissionStatus.Failed

  private def fromProtocolVerdict(verdict: judgeprotocol.model.SubmissionVerdict): SubmissionVerdict =
    verdict match
      case judgeprotocol.model.SubmissionVerdict.Accepted => SubmissionVerdict.Accepted
      case judgeprotocol.model.SubmissionVerdict.WrongAnswer => SubmissionVerdict.WrongAnswer
      case judgeprotocol.model.SubmissionVerdict.CompileError => SubmissionVerdict.CompileError
      case judgeprotocol.model.SubmissionVerdict.RuntimeError => SubmissionVerdict.RuntimeError
      case judgeprotocol.model.SubmissionVerdict.TimeLimitExceeded => SubmissionVerdict.TimeLimitExceeded
      case judgeprotocol.model.SubmissionVerdict.SystemError => SubmissionVerdict.SystemError
