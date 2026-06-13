package domains.problem.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.response.ProblemDetail
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

/** 题目访问规则；把共享访问策略、题单可见性和全局题目管理员权限合成为领域权限。 */
object ProblemAccessRules:

  /** 单个题目对当前用户的可见和可管理判断结果。 */
  final case class ProblemPermissionEvaluation(
    canView: Boolean,
    canManage: Boolean
  )

  /** 如果用户可见则返回带 canManage 标记的题目详情，否则返回 None。 */
  def enrichProblemPermissions(
    actor: AuthenticatedUser,
    problem: ProblemDetail,
    actorGroupSlugs: Set[UserGroupSlug],
    hasVisibleContainingProblemSet: Boolean
  ): Option[ProblemDetail] =
    val decision = evaluateProblemPermissions(
      actor,
      problem,
      actorGroupSlugs,
      hasVisibleContainingProblemSet
    )
    if decision.canView then Some(problem.copy(canManage = decision.canManage)) else None

  /** 评估题目可见/可管理权限；可见性包含直接策略、管理员和可见题单包含关系。 */
  def evaluateProblemPermissions(
    actor: AuthenticatedUser,
    problem: ProblemDetail,
    actorGroupSlugs: Set[UserGroupSlug],
    hasVisibleContainingProblemSet: Boolean
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
          resourceDecision.canViewDirectly ||
          hasVisibleContainingProblemSet,
      canManage = canManage
    )

  /** 单独判断题目管理权限；不考虑题单带来的普通可见性。 */
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

  /** 题目管理员拥有所有题目的全局查看覆盖。 */
  def hasGlobalViewOverride(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  /** 题目管理员拥有所有题目的全局管理覆盖。 */
  def hasGlobalManageOverride(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
