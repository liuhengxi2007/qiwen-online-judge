package domains.judge.application

import cats.effect.IO
import database.DatabaseSession
import domains.judge.model.{JudgeTask, JudgerName, ReportJudgeResultRequest}
import domains.submission.model.{SubmissionId, SubmissionStatus, SubmissionVerdict}
import domains.submission.table.SubmissionTable

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
      case SubmissionStatus.Completed | SubmissionStatus.Failed =>
        databaseSession.withTransactionConnection { connection =>
          SubmissionTable.findById(connection, submissionId).flatMap {
            case None =>
              IO.pure(ReportJudgeResult.SubmissionNotFound)
            case Some(_) =>
              SubmissionTable
                .markCompleted(connection, submissionId, request.status, request.verdict, request.judgeMessage)
                .as(ReportJudgeResult.Updated)
          }
        }
      case _ =>
        IO.pure(ReportJudgeResult.ValidationFailed("Judge results may only set status to completed or failed."))
