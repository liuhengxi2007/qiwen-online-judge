package domains.problem.http

import domains.problem.http.mapper.ProblemHttpResponseMappers



import domains.problem.application.ProblemDataStorage
import shared.http.AuthenticatedHttpPlanRegistry

object ProblemHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listProblems: Plain[domains.auth.model.AuthUser, domains.problem.model.request.ProblemListRequest, shared.model.PageResponse[domains.problem.model.response.ProblemSummary]],
    listProblemSuggestions: Plain[domains.auth.model.AuthUser, domains.problem.model.request.ProblemSearchQuery, List[domains.problem.model.response.ProblemSuggestion]],
    createProblem: WithTransaction[domains.auth.model.AuthUser, domains.problem.model.request.CreateProblemRequest, domains.problem.application.ProblemCommands.CreateProblemResult],
    getProblem: Plain[domains.auth.model.AuthUser, domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.GetProblemResult],
    listProblemData: Plain[domains.auth.model.AuthUser, domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.ListProblemDataResult],
    listProblemDataTree: Plain[domains.auth.model.AuthUser, domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.ListProblemDataTreeResult],
    downloadProblemData: Plain[domains.auth.model.AuthUser, (domains.problem.model.ProblemSlug, domains.problem.model.ProblemDataFilename), domains.problem.http.ProblemHttpPlans.DownloadProblemDataOutput],
    deleteProblemData: WithTransaction[domains.auth.model.AuthUser, (domains.problem.model.ProblemSlug, domains.problem.model.ProblemDataFilename), domains.problem.application.ProblemCommands.DeleteProblemDataResult],
    deleteProblemDataPath: WithTransaction[domains.auth.model.AuthUser, (domains.problem.model.ProblemSlug, domains.problem.model.request.DeleteProblemDataPathRequest), domains.problem.application.ProblemCommands.DeleteProblemDataResult],
    clearProblemData: WithTransaction[domains.auth.model.AuthUser, domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.ClearProblemDataResult],
    setProblemReady: WithTransaction[domains.auth.model.AuthUser, (domains.problem.model.ProblemSlug, domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest), domains.problem.application.ProblemCommands.SetProblemReadyResult],
    updateProblem: WithTransaction[domains.auth.model.AuthUser, (domains.problem.model.ProblemSlug, domains.problem.model.request.UpdateProblemRequest), domains.problem.application.ProblemCommands.UpdateProblemResult],
    deleteProblem: WithTransaction[domains.auth.model.AuthUser, domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.DeleteProblemResult]
  )

  def plans(problemDataStorage: ProblemDataStorage): RegisteredPlans =
    RegisteredPlans(
      listProblems = Plain(ProblemHttpPlans.ListProblems, ProblemHttpResponseMappers.listProblemsResponse),
      listProblemSuggestions = Plain(ProblemHttpPlans.ListProblemSuggestions, ProblemHttpResponseMappers.listProblemSuggestionsResponse),
      createProblem = WithTransaction(ProblemHttpPlans.CreateProblem, ProblemHttpResponseMappers.mapCreateResult),
      getProblem = Plain(ProblemHttpPlans.GetProblem, ProblemHttpResponseMappers.mapGetResult),
      listProblemData = Plain(new ProblemHttpPlans.ListProblemDataPlan(problemDataStorage), ProblemHttpResponseMappers.mapListDataResult),
      listProblemDataTree = Plain(ProblemHttpPlans.ListProblemDataTree, ProblemHttpResponseMappers.mapListDataTreeResult),
      downloadProblemData = Plain(
        new ProblemHttpPlans.DownloadProblemDataPlan(problemDataStorage),
        output => ProblemHttpResponseMappers.downloadOutputResponse(problemDataStorage, output)
      ),
      deleteProblemData = WithTransaction(new ProblemHttpPlans.DeleteProblemDataPlan(problemDataStorage), ProblemHttpResponseMappers.mapDeleteDataResult),
      deleteProblemDataPath = WithTransaction(new ProblemHttpPlans.DeleteProblemDataPathPlan(problemDataStorage), ProblemHttpResponseMappers.mapDeleteDataResult),
      clearProblemData = WithTransaction(new ProblemHttpPlans.ClearProblemDataPlan(problemDataStorage), ProblemHttpResponseMappers.mapClearDataResult),
      setProblemReady = WithTransaction(new ProblemHttpPlans.SetProblemReadyPlan(problemDataStorage), ProblemHttpResponseMappers.mapSetReadyResult),
      updateProblem = WithTransaction(ProblemHttpPlans.UpdateProblem, ProblemHttpResponseMappers.mapUpdateResult),
      deleteProblem = WithTransaction(ProblemHttpPlans.DeleteProblem, ProblemHttpResponseMappers.mapDeleteResult)
    )
