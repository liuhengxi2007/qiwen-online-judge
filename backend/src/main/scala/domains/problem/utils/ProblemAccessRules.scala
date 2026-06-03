package domains.problem.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.response.ProblemDetail
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

object ProblemAccessRules:

  final case class ProblemPermissionEvaluation(
    canView: Boolean,
    canManage: Boolean
  )

  def enrichProblemPermissions(
    actor: AuthenticatedUser,
    problem: ProblemDetail,
    actorGroupSlugs: Set[UserGroupSlug],
    hasVisibleContainingProblemSet: Boolean,
    hasVisibleUnfinishedContestContainingProblem: Boolean,
    hasVisibleEndedContestContainingProblem: Boolean
  ): Option[ProblemDetail] =
    val decision = evaluateProblemPermissions(
      actor,
      problem,
      actorGroupSlugs,
      hasVisibleContainingProblemSet,
      hasVisibleUnfinishedContestContainingProblem,
      hasVisibleEndedContestContainingProblem
    )
    if decision.canView then Some(problem.copy(canManage = decision.canManage)) else None

  def evaluateProblemPermissions(
    actor: AuthenticatedUser,
    problem: ProblemDetail,
    actorGroupSlugs: Set[UserGroupSlug],
    hasVisibleContainingProblemSet: Boolean,
    hasVisibleUnfinishedContestContainingProblem: Boolean,
    hasVisibleEndedContestContainingProblem: Boolean
  ): ProblemPermissionEvaluation =
    val resourceDecision =
      ResourceAccessDecision.evaluate(
        ResourceAccessFacts(
          policy = problem.accessPolicy,
          actorUsername = toAccessUsername(actor.username),
          actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs),
          hasGlobalViewOverride = hasGlobalViewOverride(actor),
          hasGlobalManageOverride = hasGlobalManageOverride(actor)
        )
      )

    val canManage = resourceDecision.canManage
    ProblemPermissionEvaluation(
      canView =
        canManage ||
          (!hasVisibleUnfinishedContestContainingProblem &&
            (resourceDecision.canViewDirectly || hasVisibleContainingProblemSet || hasVisibleEndedContestContainingProblem)),
      canManage = canManage
    )

  def canManageProblem(
    actor: AuthenticatedUser,
    problem: ProblemDetail,
    actorGroupSlugs: Set[UserGroupSlug]
  ): Boolean =
    ResourceAccessDecision
      .evaluate(
        ResourceAccessFacts(
          policy = problem.accessPolicy,
          actorUsername = toAccessUsername(actor.username),
          actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs),
          hasGlobalViewOverride = hasGlobalViewOverride(actor),
          hasGlobalManageOverride = hasGlobalManageOverride(actor)
        )
      )
      .canManage

  def hasGlobalViewOverride(actor: AuthenticatedUser): Boolean =
    actor.siteManager || actor.problemManager

  def hasGlobalManageOverride(actor: AuthenticatedUser): Boolean =
    actor.siteManager || actor.problemManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
