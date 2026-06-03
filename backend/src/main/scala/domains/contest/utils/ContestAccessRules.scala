package domains.contest.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.Contest
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

object ContestAccessRules:

  def canCreateContests(actor: AuthenticatedUser): Boolean =
    actor.siteManager || actor.contestManager

  def canViewContest(actor: AuthenticatedUser, contest: Contest, actorGroupSlugs: Set[UserGroupSlug]): Boolean =
    val decision = evaluateContestPermissions(actor, contest, actorGroupSlugs)
    decision.canViewDirectly || decision.canManage

  def canManageContest(actor: AuthenticatedUser, contest: Contest, actorGroupSlugs: Set[UserGroupSlug]): Boolean =
    evaluateContestPermissions(actor, contest, actorGroupSlugs).canManage

  def evaluateContestPermissions(
    actor: AuthenticatedUser,
    contest: Contest,
    actorGroupSlugs: Set[UserGroupSlug]
  ): ResourceAccessDecision =
    ResourceAccessDecision.evaluate(
      ResourceAccessFacts(
        policy = contest.accessPolicy,
        actorUsername = toAccessUsername(actor.username),
        actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs),
        hasGlobalViewOverride = hasGlobalOverride(actor),
        hasGlobalManageOverride = hasGlobalOverride(actor)
      )
    )

  private def hasGlobalOverride(actor: AuthenticatedUser): Boolean =
    actor.siteManager || actor.contestManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
