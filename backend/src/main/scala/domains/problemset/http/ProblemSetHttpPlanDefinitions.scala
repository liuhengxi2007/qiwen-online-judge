package domains.problemset.http

import domains.problemset.http.response.ProblemSetHttpResponses



import domains.auth.model.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object ProblemSetHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listProblemSets: PlainPlan[shared.model.PageRequest, shared.model.PageResponse[domains.problemset.application.output.ProblemSetSummary]] =
    Plain(ProblemSetHttpPlans.ListProblemSets, ProblemSetHttpResponses.listProblemSetsResponse)
  val getProblemSet: PlainPlan[domains.problemset.model.ProblemSetSlug, domains.problemset.application.ProblemSetCommands.GetProblemSetResult] =
    Plain(ProblemSetHttpPlans.GetProblemSet, ProblemSetHttpResponses.mapGetResult)
  val createProblemSet: TransactionPlan[domains.problemset.application.input.CreateProblemSetRequest, domains.problemset.application.ProblemSetCommands.CreateProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.CreateProblemSet, ProblemSetHttpResponses.mapCreateResult)
  val addProblem: TransactionPlan[(domains.problemset.model.ProblemSetSlug, domains.problemset.application.input.AddProblemToProblemSetRequest), domains.problemset.application.ProblemSetCommands.AddProblemResult] =
    WithTransaction(ProblemSetHttpPlans.AddProblem, ProblemSetHttpResponses.mapAddProblemResult)
  val updateProblemSet: TransactionPlan[(domains.problemset.model.ProblemSetSlug, domains.problemset.application.input.UpdateProblemSetRequest), domains.problemset.application.ProblemSetCommands.UpdateProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.UpdateProblemSet, ProblemSetHttpResponses.mapUpdateResult)
  val deleteProblemSet: TransactionPlan[domains.problemset.model.ProblemSetSlug, domains.problemset.application.ProblemSetCommands.DeleteProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.DeleteProblemSet, ProblemSetHttpResponses.mapDeleteResult)
  val removeProblem: TransactionPlan[(domains.problemset.model.ProblemSetSlug, domains.problem.model.ProblemSlug), domains.problemset.application.ProblemSetCommands.RemoveProblemResult] =
    WithTransaction(ProblemSetHttpPlans.RemoveProblem, ProblemSetHttpResponses.mapRemoveProblemResult)

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
