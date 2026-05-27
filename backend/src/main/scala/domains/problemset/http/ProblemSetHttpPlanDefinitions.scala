package domains.problemset.http

import domains.problemset.http.mapper.ProblemSetHttpResponseMappers



import domains.auth.objects.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object ProblemSetHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listProblemSets: PlainPlan[shared.objects.PageRequest, shared.objects.PageResponse[domains.problemset.objects.response.ProblemSetSummary]] =
    Plain(ProblemSetHttpPlans.ListProblemSets, ProblemSetHttpResponseMappers.listProblemSetsResponse)
  val getProblemSet: PlainPlan[domains.problemset.objects.ProblemSetSlug, domains.problemset.application.ProblemSetCommands.GetProblemSetResult] =
    Plain(ProblemSetHttpPlans.GetProblemSet, ProblemSetHttpResponseMappers.mapGetResult)
  val createProblemSet: TransactionPlan[domains.problemset.objects.request.CreateProblemSetRequest, domains.problemset.application.ProblemSetCommands.CreateProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.CreateProblemSet, ProblemSetHttpResponseMappers.mapCreateResult)
  val addProblem: TransactionPlan[(domains.problemset.objects.ProblemSetSlug, domains.problemset.objects.request.AddProblemToProblemSetRequest), domains.problemset.application.ProblemSetCommands.AddProblemResult] =
    WithTransaction(ProblemSetHttpPlans.AddProblem, ProblemSetHttpResponseMappers.mapAddProblemResult)
  val updateProblemSet: TransactionPlan[(domains.problemset.objects.ProblemSetSlug, domains.problemset.objects.request.UpdateProblemSetRequest), domains.problemset.application.ProblemSetCommands.UpdateProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.UpdateProblemSet, ProblemSetHttpResponseMappers.mapUpdateResult)
  val deleteProblemSet: TransactionPlan[domains.problemset.objects.ProblemSetSlug, domains.problemset.application.ProblemSetCommands.DeleteProblemSetResult] =
    WithTransaction(ProblemSetHttpPlans.DeleteProblemSet, ProblemSetHttpResponseMappers.mapDeleteResult)
  val removeProblem: TransactionPlan[(domains.problemset.objects.ProblemSetSlug, domains.problem.objects.ProblemSlug), domains.problemset.application.ProblemSetCommands.RemoveProblemResult] =
    WithTransaction(ProblemSetHttpPlans.RemoveProblem, ProblemSetHttpResponseMappers.mapRemoveProblemResult)
