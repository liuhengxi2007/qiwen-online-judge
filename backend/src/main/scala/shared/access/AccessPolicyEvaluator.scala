package shared.access

object AccessPolicyEvaluator:
  def canView(
    policy: ResourceAccessPolicy,
    viewerUsername: AccessUsername,
    viewerGroupSlugs: Set[AccessUserGroupSlug],
    isOwner: Boolean,
    hasGlobalOverride: Boolean
  ): Boolean =
    isOwner ||
      hasGlobalOverride ||
      policy.baseAccess == BaseAccess.Public ||
      policy.viewerGrants.exists {
        case AccessSubject.User(username) =>
          username.value == viewerUsername.value
        case AccessSubject.UserGroup(slug) =>
          viewerGroupSlugs.contains(slug)
      }

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
