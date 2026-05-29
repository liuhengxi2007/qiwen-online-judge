package domains.problem.rules

import cats.effect.IO
import domains.auth.objects.AuthUser
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemQueryTable
import domains.user.objects.Username
import domains.usergroup.api.ListUserGroupSlugsForMember
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

import java.sql.Connection

object ProblemAccessRules:

  final case class ProblemPermissionEvaluation(
    canView: Boolean,
    canManage: Boolean
  )

  def enrichProblemPermissions(
    connection: Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Option[ProblemDetail]] =
    evaluateProblemPermissions(connection, actor, problem).map { decision =>
      if decision.canView then Some(problem.copy(canManage = decision.canManage)) else None
    }

  def evaluateProblemPermissions(
    connection: Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[ProblemPermissionEvaluation] =
    ListUserGroupSlugsForMember.plan(connection, actor.username).flatMap { actorGroupSlugs =>
      val resourceAccessFacts = ResourceAccessFacts(
        policy = problem.accessPolicy,
        actorUsername = toAccessUsername(actor.username),
        actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs.slugs.toSet),
        hasGlobalViewOverride = hasGlobalViewOverride(actor),
        hasGlobalManageOverride = hasGlobalManageOverride(actor)
      )

      ProblemQueryTable.hasVisibleContainingProblemSet(connection, actor, problem.id).map { hasVisibleContainingProblemSet =>
        val resourceDecision = ResourceAccessDecision.evaluate(resourceAccessFacts)

        ProblemPermissionEvaluation(
          canView = resourceDecision.canViewDirectly || hasVisibleContainingProblemSet,
          canManage = resourceDecision.canManage
        )
      }
    }

  def canManageProblem(
    connection: Connection,
    actor: AuthUser,
    problem: ProblemDetail
  ): IO[Boolean] =
    ListUserGroupSlugsForMember.plan(connection, actor.username).map { actorGroupSlugs =>
      ResourceAccessDecision
        .evaluate(
          ResourceAccessFacts(
            policy = problem.accessPolicy,
            actorUsername = toAccessUsername(actor.username),
            actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs.slugs.toSet),
            hasGlobalViewOverride = hasGlobalViewOverride(actor),
            hasGlobalManageOverride = hasGlobalManageOverride(actor)
          )
        )
        .canManage
    }

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def hasGlobalManageOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
