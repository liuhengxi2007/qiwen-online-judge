package shared.application.access

import shared.model.access.{AccessUserGroupSlug, AccessUsername, ResourceAccessPolicy}

final case class ResourceAccessFacts(
  policy: ResourceAccessPolicy,
  actorUsername: AccessUsername,
  actorGroupSlugs: Set[AccessUserGroupSlug],
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
