package domains.problemset.http

import domains.problemset.http.mapper.ProblemSetHttpResponseMappers



import domains.auth.model.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object ProblemSetHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listProblemSets: PlainPlan[shared.model.PageRequest, shared.model.PageResponse[domains.problemset.model.response.ProblemSetSummary]] =
    Plain(ProblemSetHttpPlans.ListProblemSets, ProblemSetHttpResponseMappers.listProblemSetsResponse)
  val getProblemSet: PlainPlan[domains.problemset.model.ProblemSetSlug, domains.problemset.application.ProblemSetCommands.GetProblemSetResult] =
    Plain(ProblemSetHttpPlans.GetProblemSet, ProblemSetHttpResponseMappers.mapGetResult)
  val createProblemSet: TransactionPlan[domains.problemset.model.request.CreateProblemSetRequest, domains.problemset.application.ProblemSetCommands.CreateProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.CreateProblemSet, ProblemSetHttpResponseMappers.mapCreateResult)
  val addProblem: TransactionPlan[(domains.problemset.model.ProblemSetSlug, domains.problemset.model.request.AddProblemToProblemSetRequest), domains.problemset.application.ProblemSetCommands.AddProblemResult] =
    WithTransaction(ProblemSetHttpPlans.AddProblem, ProblemSetHttpResponseMappers.mapAddProblemResult)
  val updateProblemSet: TransactionPlan[(domains.problemset.model.ProblemSetSlug, domains.problemset.model.request.UpdateProblemSetRequest), domains.problemset.application.ProblemSetCommands.UpdateProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.UpdateProblemSet, ProblemSetHttpResponseMappers.mapUpdateResult)
  val deleteProblemSet: TransactionPlan[domains.problemset.model.ProblemSetSlug, domains.problemset.application.ProblemSetCommands.DeleteProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.DeleteProblemSet, ProblemSetHttpResponseMappers.mapDeleteResult)
  val removeProblem: TransactionPlan[(domains.problemset.model.ProblemSetSlug, domains.problem.model.ProblemSlug), domains.problemset.application.ProblemSetCommands.RemoveProblemResult] =
    WithTransaction(ProblemSetHttpPlans.RemoveProblem, ProblemSetHttpResponseMappers.mapRemoveProblemResult)

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
