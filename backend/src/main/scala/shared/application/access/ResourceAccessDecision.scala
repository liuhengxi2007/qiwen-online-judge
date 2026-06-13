package shared.application.access

import shared.objects.access.{AccessUserGroupSlug, AccessUsername, ResourceAccessPolicy}

/** 资源访问决策所需的输入事实，包含策略、访问者身份、用户组和全局覆盖权限。 */
final case class ResourceAccessFacts(
  policy: ResourceAccessPolicy,
  actorUsername: AccessUsername,
  actorGroupSlugs: Set[AccessUserGroupSlug],
  hasGlobalViewOverride: Boolean,
  hasGlobalManageOverride: Boolean
)

/** 资源访问决策结果，区分直接查看权限和管理权限。 */
final case class ResourceAccessDecision(
  canViewDirectly: Boolean,
  canManage: Boolean
)

/** 将访问事实转换为可被业务层消费的权限决策。 */
object ResourceAccessDecision:

  /** 使用共享访问策略求值器计算查看和管理结果，无副作用。 */
  def evaluate(facts: ResourceAccessFacts): ResourceAccessDecision =
    ResourceAccessDecision(
      canViewDirectly = AccessPolicyEvaluator.canView(
        policy = facts.policy,
        viewerUsername = facts.actorUsername,
        viewerGroupSlugs = facts.actorGroupSlugs,
        hasGlobalOverride = facts.hasGlobalViewOverride
      ),
      canManage = AccessPolicyEvaluator.canManage(
        policy = facts.policy,
        actorUsername = facts.actorUsername,
        actorGroupSlugs = facts.actorGroupSlugs,
        hasGlobalOverride = facts.hasGlobalManageOverride
      )
    )
