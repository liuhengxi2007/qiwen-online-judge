package domains.submission.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.problem.model.OthersSubmissionAccess
import domains.problem.table.ProblemTable
import domains.submission.model.SubmissionId
import domains.submission.table.SubmissionTable
import domains.submission.application.SubmissionCommandResults.*
import domains.submission.application.SubmissionCommandSupport.*

object SubmissionQueryCommands:

  def listSubmissions(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    submitterUsername: Option[Username]
  ): IO[ListSubmissionsResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionTable
        .listVisibleTo(connection, actor, submitterUsername)
        .map(submissions => ListSubmissionsResult.Listed(submissions))
    }

  def getSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    submissionId: SubmissionId
  ): IO[GetSubmissionResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionTable.findById(connection, submissionId).flatMap {
        case None =>
          IO.pure(GetSubmissionResult.NotFound)
        case Some(submission) if SubmissionPolicy.canViewOwnOrWithGlobalOverride(actor, submission.submitterUsername) =>
          IO.pure(GetSubmissionResult.Found(submission))
        case Some(submission) =>
          ProblemTable.findBySlug(connection, submission.problemSlug).flatMap {
            case None =>
              IO.pure(GetSubmissionResult.NotFound)
            case Some(problem) =>
              canViewOthersSubmission(connection, actor, problem, OthersSubmissionAccess.Detail).map {
                case true => GetSubmissionResult.Found(submission)
                case false => GetSubmissionResult.Forbidden
              }
          }
      }
    }
