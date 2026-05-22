package domains.submission.application.utils



import domains.submission.application.SubmissionPolicy
import cats.effect.IO
import domains.auth.model.AuthUser
import domains.problem.model.{OthersSubmissionAccess}
import domains.problem.application.output.{ProblemDetail}
import domains.problem.table.ProblemTable
import domains.shared.access.AccessPolicyEvaluator
import domains.usergroup.table.UserGroupTable

object SubmissionCommandSupport:

  def canSubmitToProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    canViewProblem(connection, actor, problem)

  def canViewOthersSubmission(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail,
    minimumAccess: OthersSubmissionAccess
  ): IO[Boolean] =
    canViewProblem(connection, actor, problem).map {
      case false => false
      case true =>
        minimumAccess match
          case OthersSubmissionAccess.Detail => SubmissionPolicy.canViewDetailOfOthers(problem.othersSubmissionAccess)
          case OthersSubmissionAccess.Summary => SubmissionPolicy.canViewSummaryOfOthers(problem.othersSubmissionAccess)
          case OthersSubmissionAccess.None => true
    }

  def canViewProblem(
    connection: java.sql.Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    UserGroupTable.listGroupSlugsForMember(connection, actor.username).flatMap { viewerGroupSlugs =>
      val canViewDirectly = AccessPolicyEvaluator.canView(
        policy = problem.accessPolicy,
        viewerUsername = actor.username,
        viewerGroupSlugs = viewerGroupSlugs,
        isOwner = false,
        hasGlobalOverride = SubmissionPolicy.hasGlobalViewOverride(actor)
      )

      if canViewDirectly then
        IO.pure(true)
      else
        ProblemTable.hasVisibleContainingProblemSet(connection, actor, problem.id)
    }
