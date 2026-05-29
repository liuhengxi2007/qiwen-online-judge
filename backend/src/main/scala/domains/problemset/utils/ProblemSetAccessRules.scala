package domains.problemset.utils

import domains.auth.objects.AuthUser
import domains.problemset.objects.ProblemSet
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

object ProblemSetAccessRules:

  def canViewProblemSet(
    actor: AuthUser,
    problemSet: ProblemSet,
    actorGroupSlugs: Set[UserGroupSlug]
  ): Boolean =
    ResourceAccessDecision
      .evaluate(
        ResourceAccessFacts(
          policy = problemSet.accessPolicy,
          actorUsername = toAccessUsername(actor.username),
          actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs),
          hasGlobalViewOverride = hasGlobalViewOverride(actor),
          hasGlobalManageOverride = hasGlobalViewOverride(actor)
        )
      )
      .canViewDirectly

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canManageProblemSets(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
