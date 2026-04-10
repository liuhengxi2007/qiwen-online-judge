package domains.submission.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.auth.model.Username
import domains.problem.model.ProblemDetail
import domains.problem.table.ProblemTable
import domains.shared.access.AccessPolicyEvaluator
import domains.submission.model.{CreateSubmissionRequest, SubmissionDetail, SubmissionId, SubmissionSummary}
import domains.submission.table.SubmissionTable
import domains.usergroup.table.UserGroupTable

object SubmissionCommands:

  enum CreateSubmissionResult:
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Forbidden
    case Created(submission: SubmissionDetail)

  enum ListSubmissionsResult:
    case Listed(submissions: List[SubmissionSummary])

  enum GetSubmissionResult:
    case NotFound
    case Forbidden
    case Found(submission: SubmissionDetail)

  def createSubmission(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateSubmissionRequest
  ): IO[CreateSubmissionResult] =
    SubmissionValidation.validateCreate(request) match
      case Left(message) =>
        IO.pure(CreateSubmissionResult.ValidationFailed(message))
      case Right(validRequest) =>
        databaseSession.withTransactionConnection { connection =>
          ProblemTable.findBySlug(connection, validRequest.problemSlug).flatMap {
            case None =>
              IO.pure(CreateSubmissionResult.ProblemNotFound)
            case Some(problem) =>
              canSubmitToProblem(connection, actor, problem).flatMap {
                case false =>
                  IO.pure(CreateSubmissionResult.Forbidden)
                case true =>
                  SubmissionTable
                    .insert(
                      connection = connection,
                      problemId = problem.id,
                      problemSlug = problem.slug,
                      submitterUsername = actor.username,
                      language = validRequest.language,
                      sourceCode = validRequest.sourceCode,
                    )
                    .map(submission => CreateSubmissionResult.Created(submission))
              }
          }
        }

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
      SubmissionTable.findById(connection, submissionId).map {
        case None =>
          GetSubmissionResult.NotFound
        case Some(submission) if SubmissionPolicy.canView(actor, submission.submitterUsername) =>
          GetSubmissionResult.Found(submission)
        case Some(_) =>
          GetSubmissionResult.Forbidden
      }
    }

  private def canSubmitToProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    UserGroupTable.listGroupSlugsForMember(connection, actor.username).flatMap { viewerGroupSlugs =>
      val canViewDirectly = AccessPolicyEvaluator.canView(
        policy = problem.accessPolicy,
        viewerUsername = actor.username,
        viewerGroupSlugs = viewerGroupSlugs,
        isOwner = problem.ownerUsername.value == actor.username.value,
        hasGlobalOverride = SubmissionPolicy.hasGlobalViewOverride(actor)
      )

      if canViewDirectly then
        IO.pure(true)
      else
        ProblemTable.hasVisibleContainingProblemSet(connection, actor, problem.id)
    }
