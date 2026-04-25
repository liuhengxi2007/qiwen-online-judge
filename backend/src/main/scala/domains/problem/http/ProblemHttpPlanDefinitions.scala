package domains.problem.http

import domains.shared.http.AuthenticatedHttpPlanRegistry

object ProblemHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listProblems = Plain(ProblemHttpPlans.ListProblems, ProblemHttpResponses.listProblemsResponse)
  val listProblemSuggestions = Plain(ProblemHttpPlans.ListProblemSuggestions, ProblemHttpResponses.listProblemSuggestionsResponse)
  val createProblem = WithTransaction(ProblemHttpPlans.CreateProblem, ProblemHttpResponses.mapCreateResult)
  val getProblem = Plain(ProblemHttpPlans.GetProblem, ProblemHttpResponses.mapGetResult)
  val listProblemData = Plain(ProblemHttpPlans.ListProblemData, ProblemHttpResponses.mapListDataResult)
  val downloadProblemData = Plain(ProblemHttpPlans.DownloadProblemData, ProblemHttpResponses.downloadOutputResponse)
  val deleteProblemData = WithTransaction(ProblemHttpPlans.DeleteProblemData, ProblemHttpResponses.mapDeleteDataResult)
  val clearProblemData = WithTransaction(ProblemHttpPlans.ClearProblemData, ProblemHttpResponses.mapClearDataResult)
  val updateProblem = WithTransaction(ProblemHttpPlans.UpdateProblem, ProblemHttpResponses.mapUpdateResult)
  val updateProblemData = WithTransaction(ProblemHttpPlans.UpdateProblemData, ProblemHttpResponses.mapUpdateDataResult)
  val deleteProblem = WithTransaction(ProblemHttpPlans.DeleteProblem, ProblemHttpResponses.mapDeleteResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listProblems,
      listProblemSuggestions,
      createProblem,
      getProblem,
      listProblemData,
      downloadProblemData,
      deleteProblemData,
      clearProblemData,
      updateProblem,
      updateProblemData,
      deleteProblem
    ).map(plan => plan.name -> plan).toMap
