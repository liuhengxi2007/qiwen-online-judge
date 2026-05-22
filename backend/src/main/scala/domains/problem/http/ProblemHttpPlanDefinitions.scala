package domains.problem.http

import domains.problem.http.response.ProblemHttpResponses



import domains.problem.application.ProblemDataStorage
import shared.http.AuthenticatedHttpPlanRegistry

object ProblemHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listProblems: Plain[domains.problem.application.input.ProblemListRequest, shared.model.PageResponse[domains.problem.application.output.ProblemSummary]],
    listProblemSuggestions: Plain[domains.problem.model.ProblemSearchQuery, List[domains.problem.application.output.ProblemSuggestion]],
    createProblem: WithTransaction[domains.problem.application.input.CreateProblemRequest, domains.problem.application.ProblemCommands.CreateProblemResult],
    getProblem: Plain[domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.GetProblemResult],
    listProblemData: Plain[domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.ListProblemDataResult],
    listProblemDataTree: Plain[domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.ListProblemDataTreeResult],
    downloadProblemData: Plain[(domains.problem.model.ProblemSlug, domains.problem.model.ProblemDataFilename), domains.problem.http.ProblemHttpPlans.DownloadProblemDataOutput],
    deleteProblemData: WithTransaction[(domains.problem.model.ProblemSlug, domains.problem.model.ProblemDataFilename), domains.problem.application.ProblemCommands.DeleteProblemDataResult],
    deleteProblemDataPath: WithTransaction[(domains.problem.model.ProblemSlug, domains.problem.application.input.DeleteProblemDataPathRequest), domains.problem.application.ProblemCommands.DeleteProblemDataResult],
    clearProblemData: WithTransaction[domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.ClearProblemDataResult],
    setProblemReady: WithTransaction[(domains.problem.model.ProblemSlug, domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest), domains.problem.application.ProblemCommands.SetProblemReadyResult],
    updateProblem: WithTransaction[(domains.problem.model.ProblemSlug, domains.problem.application.input.UpdateProblemRequest), domains.problem.application.ProblemCommands.UpdateProblemResult],
    deleteProblem: WithTransaction[domains.problem.model.ProblemSlug, domains.problem.application.ProblemCommands.DeleteProblemResult]
  )

  def plans(problemDataStorage: ProblemDataStorage): RegisteredPlans =
    RegisteredPlans(
      listProblems = Plain(ProblemHttpPlans.ListProblems, ProblemHttpResponses.listProblemsResponse),
      listProblemSuggestions = Plain(ProblemHttpPlans.ListProblemSuggestions, ProblemHttpResponses.listProblemSuggestionsResponse),
      createProblem = WithTransaction(ProblemHttpPlans.CreateProblem, ProblemHttpResponses.mapCreateResult),
      getProblem = Plain(ProblemHttpPlans.GetProblem, ProblemHttpResponses.mapGetResult),
      listProblemData = Plain(new ProblemHttpPlans.ListProblemDataPlan(problemDataStorage), ProblemHttpResponses.mapListDataResult),
      listProblemDataTree = Plain(ProblemHttpPlans.ListProblemDataTree, ProblemHttpResponses.mapListDataTreeResult),
      downloadProblemData = Plain(
        new ProblemHttpPlans.DownloadProblemDataPlan(problemDataStorage),
        output => ProblemHttpResponses.downloadOutputResponse(problemDataStorage, output)
      ),
      deleteProblemData = WithTransaction(new ProblemHttpPlans.DeleteProblemDataPlan(problemDataStorage), ProblemHttpResponses.mapDeleteDataResult),
      deleteProblemDataPath = WithTransaction(new ProblemHttpPlans.DeleteProblemDataPathPlan(problemDataStorage), ProblemHttpResponses.mapDeleteDataResult),
      clearProblemData = WithTransaction(new ProblemHttpPlans.ClearProblemDataPlan(problemDataStorage), ProblemHttpResponses.mapClearDataResult),
      setProblemReady = WithTransaction(new ProblemHttpPlans.SetProblemReadyPlan(problemDataStorage), ProblemHttpResponses.mapSetReadyResult),
      updateProblem = WithTransaction(ProblemHttpPlans.UpdateProblem, ProblemHttpResponses.mapUpdateResult),
      deleteProblem = WithTransaction(ProblemHttpPlans.DeleteProblem, ProblemHttpResponses.mapDeleteResult)
    )
