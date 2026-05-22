package domains.submission.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.OthersSubmissionAccess
import domains.problem.application.utils.ProblemCommandSupport.canManageProblem
import domains.problem.table.ProblemTable
import domains.submission.model.{SubmissionId}
import domains.submission.application.input.{SubmissionListRequest}
import domains.submission.table.SubmissionTable
import domains.submission.application.SubmissionCommandResults.*
import domains.submission.application.utils.SubmissionCommandSupport.*

object SubmissionQueryCommands:

  def listSubmissions(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: SubmissionListRequest
  ): IO[ListSubmissionsResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionTable
        .listVisibleTo(connection, actor, request, SubmissionPolicy.hasGlobalViewOverride(actor))
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
        case Some(submission) if SubmissionPolicy.canViewOwnOrWithGlobalOverride(actor, submission.submitter.username) =>
          ProblemTable.findBySlug(connection, submission.problemSlug).flatMap {
            case None =>
              IO.pure(GetSubmissionResult.NotFound)
            case Some(problem) =>
              canManageProblem(connection, actor, problem).map(manageable =>
                GetSubmissionResult.Found(submission.copy(canManage = manageable))
              )
          }
        case Some(submission) =>
          ProblemTable.findBySlug(connection, submission.problemSlug).flatMap {
            case None =>
              IO.pure(GetSubmissionResult.NotFound)
            case Some(problem) =>
              canViewOthersSubmission(connection, actor, problem, OthersSubmissionAccess.Detail).flatMap {
                case false =>
                  IO.pure(GetSubmissionResult.Forbidden)
                case true =>
                  canManageProblem(connection, actor, problem).map(manageable =>
                    GetSubmissionResult.Found(submission.copy(canManage = manageable))
                  )
              }
          }
      }
    }
