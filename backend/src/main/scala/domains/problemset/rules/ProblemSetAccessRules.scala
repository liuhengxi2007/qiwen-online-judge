package domains.problemset.rules

import cats.effect.IO
import domains.auth.objects.AuthUser
import domains.problemset.objects.ProblemSet
import domains.user.objects.Username
import domains.usergroup.api.ListUserGroupSlugsForMember
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

import java.sql.Connection

object ProblemSetAccessRules:

  def canViewProblemSet(
    connection: Connection,
    actor: AuthUser,
    problemSet: ProblemSet
  ): IO[Boolean] =
    ListUserGroupSlugsForMember.plan(connection, actor.username).map { viewerGroupSlugs =>
      ResourceAccessDecision
        .evaluate(
          ResourceAccessFacts(
            policy = problemSet.accessPolicy,
            actorUsername = toAccessUsername(actor.username),
            actorGroupSlugs = toAccessGroupSlugs(viewerGroupSlugs.slugs.toSet),
            hasGlobalViewOverride = hasGlobalViewOverride(actor),
            hasGlobalManageOverride = hasGlobalViewOverride(actor)
          )
        )
        .canViewDirectly
    }

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canManageProblemSets(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
