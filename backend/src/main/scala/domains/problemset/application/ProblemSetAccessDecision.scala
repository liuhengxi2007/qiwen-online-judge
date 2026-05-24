package domains.problemset.application



import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}

final case class ProblemSetAccessFacts(
  resourceAccess: ResourceAccessFacts
)

final case class ProblemSetAccessDecision(
  canView: Boolean,
  canManage: Boolean
)

object ProblemSetAccessDecision:

  def evaluate(facts: ProblemSetAccessFacts): ProblemSetAccessDecision =
    val resourceDecision = ResourceAccessDecision.evaluate(facts.resourceAccess)
    ProblemSetAccessDecision(
      canView = resourceDecision.canViewDirectly,
      canManage = resourceDecision.canManage
    )
