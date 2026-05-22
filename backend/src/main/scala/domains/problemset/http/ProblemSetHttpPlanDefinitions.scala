package domains.problemset.http

import domains.problemset.http.response.ProblemSetHttpResponses



import domains.shared.http.AuthenticatedHttpPlanRegistry

object ProblemSetHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listProblemSets = Plain(ProblemSetHttpPlans.ListProblemSets, ProblemSetHttpResponses.listProblemSetsResponse)
  val getProblemSet = Plain(ProblemSetHttpPlans.GetProblemSet, ProblemSetHttpResponses.mapGetResult)
  val createProblemSet = WithTransaction(ProblemSetHttpPlans.CreateProblemSet, ProblemSetHttpResponses.mapCreateResult)
  val addProblem = WithTransaction(ProblemSetHttpPlans.AddProblem, ProblemSetHttpResponses.mapAddProblemResult)
  val updateProblemSet = WithTransaction(ProblemSetHttpPlans.UpdateProblemSet, ProblemSetHttpResponses.mapUpdateResult)
  val deleteProblemSet = WithTransaction(ProblemSetHttpPlans.DeleteProblemSet, ProblemSetHttpResponses.mapDeleteResult)
  val removeProblem = WithTransaction(ProblemSetHttpPlans.RemoveProblem, ProblemSetHttpResponses.mapRemoveProblemResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listProblemSets,
      getProblemSet,
      createProblemSet,
      addProblem,
      updateProblemSet,
      deleteProblemSet,
      removeProblem
    ).map(plan => plan.name -> plan).toMap
