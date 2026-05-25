package domains.problem.application



import cats.effect.IO
import domains.auth.model.AuthUser
import domains.problem.model.response.{ProblemAccessEvaluation, ProblemSetMemberTarget}
import domains.problem.application.utils.ProblemCommandSupport.{enrichProblemPermissions, evaluateProblemPermissions}
import domains.problem.model.{ProblemDataManifest, ProblemId, ProblemSlug}
import domains.problem.table.problem.ProblemTable
import domains.problem.table.problem_data_file.ProblemDataFileTable

import java.sql.Connection

object ProblemCommands:
  export ProblemCommandResults.*
  export ProblemQueryCommands.*
  export ProblemMutationCommands.*
  export ProblemDataCommands.*

  enum ResolveSubmissionTargetResult:
    case ProblemNotFound
    case Forbidden
    case Resolved(problem: domains.problem.model.response.ProblemDetail)

  enum EvaluateProblemAccessResult:
    case ProblemNotFound
    case Evaluated(access: ProblemAccessEvaluation)

  enum ResolveBlogProblemLinkTargetResult:
    case ProblemNotFound
    case Forbidden
    case Allowed

  def problemSlugConflictsWith(connection: Connection, rawValue: String): IO[Boolean] =
    ProblemSlug.parse(rawValue) match
      case Left(_) => IO.pure(false)
      case Right(slug) => ProblemTable.findBySlug(connection, slug).map(_.nonEmpty)

  def resolveProblemSetMemberTarget(connection: Connection, slug: ProblemSlug): IO[Option[ProblemSetMemberTarget]] =
    ProblemTable.findBySlug(connection, slug).map(_.map(problem => ProblemSetMemberTarget(problem.id)))

  def resolveSubmissionTarget(
    connection: Connection,
    actor: AuthUser,
    slug: ProblemSlug
  ): IO[ResolveSubmissionTargetResult] =
    ProblemTable.findBySlug(connection, slug).flatMap {
      case None =>
        IO.pure(ResolveSubmissionTargetResult.ProblemNotFound)
      case Some(problem) =>
        enrichProblemPermissions(connection, actor, problem).map {
          case None => ResolveSubmissionTargetResult.Forbidden
          case Some(enrichedProblem) => ResolveSubmissionTargetResult.Resolved(enrichedProblem)
        }
    }

  def evaluateProblemAccess(
    connection: Connection,
    actor: AuthUser,
    slug: ProblemSlug
  ): IO[EvaluateProblemAccessResult] =
    ProblemTable.findBySlug(connection, slug).flatMap {
      case None =>
        IO.pure(EvaluateProblemAccessResult.ProblemNotFound)
      case Some(problem) =>
        evaluateProblemPermissions(connection, actor, problem).map { permissions =>
          EvaluateProblemAccessResult.Evaluated(
            ProblemAccessEvaluation(
              canView = permissions.canView,
              canManage = permissions.canManage,
              othersSubmissionAccess = problem.othersSubmissionAccess
            )
          )
        }
    }

  def resolveBlogProblemLinkTarget(
    connection: Connection,
    actor: AuthUser,
    slug: ProblemSlug
  ): IO[ResolveBlogProblemLinkTargetResult] =
    if !ProblemPolicy.canEdit(actor) then
      IO.pure(ResolveBlogProblemLinkTargetResult.Forbidden)
    else
      ProblemTable.findBySlug(connection, slug).map {
        case None => ResolveBlogProblemLinkTargetResult.ProblemNotFound
        case Some(_) => ResolveBlogProblemLinkTargetResult.Allowed
      }

  def canManageProblemCatalog(actor: AuthUser): Boolean =
    ProblemPolicy.canEdit(actor)

  def judgeTaskManifest(
    connection: Connection,
    problemId: ProblemId,
    problemSlug: ProblemSlug
  ): IO[Option[ProblemDataManifest]] =
    ProblemTable.findBySlug(connection, problemSlug).flatMap {
      case Some(problem) if problem.id == problemId =>
        ProblemDataFileTable.manifestForProblem(connection, problemId, problemSlug).map(Some(_))
      case _ =>
        IO.pure(None)
    }
