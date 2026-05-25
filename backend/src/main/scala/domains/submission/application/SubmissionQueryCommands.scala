package domains.submission.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommands
import domains.submission.model.{SubmissionId}
import domains.submission.model.request.{SubmissionListRequest}
import domains.submission.table.submission.SubmissionQueryTable
import domains.submission.application.SubmissionCommandResults.*

object SubmissionQueryCommands:

  def listSubmissions(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: SubmissionListRequest
  ): IO[ListSubmissionsResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionQueryTable
        .listVisibleTo(connection, actor, request, SubmissionPolicy.hasGlobalViewOverride(actor))
        .map(submissions => ListSubmissionsResult.Listed(submissions))
    }

  def getSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    submissionId: SubmissionId
  ): IO[GetSubmissionResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionQueryTable.findById(connection, submissionId).flatMap {
        case None =>
          IO.pure(GetSubmissionResult.NotFound)
        case Some(submission) if SubmissionPolicy.canViewOwnOrWithGlobalOverride(actor, submission.submitter.username) =>
          ProblemCommands.evaluateProblemAccess(connection, actor, submission.problemSlug).map {
            case ProblemCommands.EvaluateProblemAccessResult.ProblemNotFound =>
              IO.pure(GetSubmissionResult.NotFound)
            case ProblemCommands.EvaluateProblemAccessResult.Evaluated(access) =>
              IO.pure(GetSubmissionResult.Found(submission.copy(canManage = access.canManage)))
          }.flatten
        case Some(submission) =>
          ProblemCommands.evaluateProblemAccess(connection, actor, submission.problemSlug).map {
            case ProblemCommands.EvaluateProblemAccessResult.ProblemNotFound =>
              IO.pure(GetSubmissionResult.NotFound)
            case ProblemCommands.EvaluateProblemAccessResult.Evaluated(access) if !access.canView =>
              IO.pure(GetSubmissionResult.Forbidden)
            case ProblemCommands.EvaluateProblemAccessResult.Evaluated(access)
                if !SubmissionPolicy.canViewDetailOfOthers(access.othersSubmissionAccess) =>
              IO.pure(GetSubmissionResult.Forbidden)
            case ProblemCommands.EvaluateProblemAccessResult.Evaluated(access) =>
              IO.pure(GetSubmissionResult.Found(submission.copy(canManage = access.canManage)))
          }
            .flatten
      }
    }
