package shared.application.access

import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, BaseAccess, ResourceAccessPolicy, ResourceVisibilityPolicy}

/** 根据资源访问策略和访问者事实计算查看/管理权限，不访问数据库。 */
object AccessPolicyEvaluator:
  /** 判断用户是否可查看资源；全局覆盖、公开资源或显式 viewer grant 任一满足即可。 */
  def canView(
    policy: ResourceAccessPolicy,
    viewerUsername: AccessUsername,
    viewerGroupSlugs: Set[AccessUserGroupSlug],
    hasGlobalOverride: Boolean
  ): Boolean =
    canViewPolicy(policy.baseAccess, policy.viewerGrants, viewerUsername, viewerGroupSlugs, hasGlobalOverride)

  /** 判断用户是否可查看 viewer-only 资源；全局覆盖、公开资源或显式 viewer grant 任一满足即可。 */
  def canView(
    policy: ResourceVisibilityPolicy,
    viewerUsername: AccessUsername,
    viewerGroupSlugs: Set[AccessUserGroupSlug],
    hasGlobalOverride: Boolean
  ): Boolean =
    canViewPolicy(policy.baseAccess, policy.viewerGrants, viewerUsername, viewerGroupSlugs, hasGlobalOverride)

  private def canViewPolicy(
    baseAccess: BaseAccess,
    viewerGrants: List[AccessSubject],
    viewerUsername: AccessUsername,
    viewerGroupSlugs: Set[AccessUserGroupSlug],
    hasGlobalOverride: Boolean
  ): Boolean =
    hasGlobalOverride ||
      baseAccess == BaseAccess.Public ||
      viewerGrants.exists {
        case AccessSubject.User(username) =>
          username.value == viewerUsername.value
        case AccessSubject.UserGroup(slug) =>
          viewerGroupSlugs.contains(slug)
      }

  /** 判断用户是否可管理资源；全局管理覆盖或显式 manager grant 满足即可。 */
  def canManage(
    policy: ResourceAccessPolicy,
    actorUsername: AccessUsername,
    actorGroupSlugs: Set[AccessUserGroupSlug],
    hasGlobalOverride: Boolean
  ): Boolean =
    hasGlobalOverride ||
      policy.managerGrants.exists {
        case AccessSubject.User(username) =>
          username.value == actorUsername.value
        case AccessSubject.UserGroup(slug) =>
          actorGroupSlugs.contains(slug)
      }
