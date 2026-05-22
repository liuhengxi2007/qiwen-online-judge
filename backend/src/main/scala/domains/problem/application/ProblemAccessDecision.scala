package domains.problem.application



import domains.shared.access.{ResourceAccessDecision, ResourceAccessFacts}

final case class ProblemAccessFacts(
  resourceAccess: ResourceAccessFacts,
  hasVisibleContainingProblemSet: Boolean
)

final case class ProblemAccessDecision(
  canView: Boolean,
  canManage: Boolean
)

object ProblemAccessDecision:

  def evaluate(facts: ProblemAccessFacts): ProblemAccessDecision =
    val resourceDecision = ResourceAccessDecision.evaluate(facts.resourceAccess)
    ProblemAccessDecision(
      canView = resourceDecision.canViewDirectly || facts.hasVisibleContainingProblemSet,
      canManage = resourceDecision.canManage
    )
