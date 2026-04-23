package domains.shared.access

import domains.auth.model.Username
import domains.usergroup.model.UserGroupSlug

object AccessPolicyEvaluator:
  def canView(
    policy: ResourceAccessPolicy,
    viewerUsername: Username,
    viewerGroupSlugs: Set[UserGroupSlug],
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
    actorUsername: Username,
    actorGroupSlugs: Set[UserGroupSlug],
    hasGlobalOverride: Boolean
  ): Boolean =
    hasGlobalOverride ||
      policy.managerGrants.exists {
        case AccessSubject.User(username) =>
          username.value == actorUsername.value
        case AccessSubject.UserGroup(slug) =>
          actorGroupSlugs.contains(slug)
      }
