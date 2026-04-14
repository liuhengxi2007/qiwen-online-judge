package domains.shared.access

import domains.auth.model.Username
import domains.usergroup.model.UserGroupSlug

final case class ResourceAccessFacts(
  policy: ResourceAccessPolicy,
  actorUsername: Username,
  actorGroupSlugs: Set[UserGroupSlug],
  hasGlobalViewOverride: Boolean,
  hasGlobalManageOverride: Boolean
)

final case class ResourceAccessDecision(
  canViewDirectly: Boolean,
  canManage: Boolean
)

object ResourceAccessDecision:

  def evaluate(facts: ResourceAccessFacts): ResourceAccessDecision =
    ResourceAccessDecision(
      canViewDirectly = AccessPolicyEvaluator.canView(
        policy = facts.policy,
        viewerUsername = facts.actorUsername,
        viewerGroupSlugs = facts.actorGroupSlugs,
        isOwner = false,
        hasGlobalOverride = facts.hasGlobalViewOverride
      ),
      canManage = AccessPolicyEvaluator.canManage(
        policy = facts.policy,
        actorUsername = facts.actorUsername,
        actorGroupSlugs = facts.actorGroupSlugs,
        hasGlobalOverride = facts.hasGlobalManageOverride
      )
    )
