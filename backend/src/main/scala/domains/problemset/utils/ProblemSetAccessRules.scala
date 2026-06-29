package domains.problemset.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.problemset.objects.ProblemSet
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.AccessPolicyEvaluator
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

/** 题单访问规则工具，集中定义查看和全局管理边界。 */
object ProblemSetAccessRules:

  /** 判断调用者是否可查看题单，题目管理员拥有全局查看覆盖。 */
  def canViewProblemSet(
    actor: AuthenticatedUser,
    problemSet: ProblemSet,
    actorGroupSlugs: Set[UserGroupSlug]
  ): Boolean =
    AccessPolicyEvaluator.canView(
      policy = problemSet.accessPolicy,
      viewerUsername = toAccessUsername(actor.username),
      viewerGroupSlugs = toAccessGroupSlugs(actorGroupSlugs),
      hasGlobalOverride = hasGlobalViewOverride(actor)
    )

  /** 判断调用者是否拥有题单全局查看覆盖权限。 */
  def hasGlobalViewOverride(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  /** 判断调用者是否能管理题单目录。 */
  def canManageProblemSets(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
