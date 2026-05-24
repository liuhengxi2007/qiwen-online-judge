package domains.submission.http

import domains.submission.http.response.SubmissionHttpResponses



import domains.auth.model.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object SubmissionHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listSubmissions: PlainPlan[domains.submission.application.input.SubmissionListRequest, domains.submission.application.SubmissionCommands.ListSubmissionsResult] =
    Plain(SubmissionHttpPlans.ListSubmissions, SubmissionHttpResponses.mapListResult)
  val createSubmission: TransactionPlan[domains.submission.application.input.CreateSubmissionRequest, domains.submission.application.SubmissionCommands.CreateSubmissionResult] =
    WithTransaction(SubmissionHttpPlans.CreateSubmission, SubmissionHttpResponses.mapCreateResult)
  val getSubmission: PlainPlan[domains.submission.model.SubmissionId, domains.submission.application.SubmissionCommands.GetSubmissionResult] =
    Plain(SubmissionHttpPlans.GetSubmission, SubmissionHttpResponses.mapGetResult)
  val deleteSubmission: TransactionPlan[domains.submission.model.SubmissionId, domains.submission.application.SubmissionCommands.DeleteSubmissionResult] =
    WithTransaction(SubmissionHttpPlans.DeleteSubmission, SubmissionHttpResponses.mapDeleteResult)
  val rejudgeSubmission: TransactionPlan[domains.submission.model.SubmissionId, domains.submission.application.SubmissionCommands.RejudgeSubmissionResult] =
    WithTransaction(SubmissionHttpPlans.RejudgeSubmission, SubmissionHttpResponses.mapRejudgeResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listSubmissions,
      createSubmission,
      getSubmission,
      deleteSubmission,
      rejudgeSubmission
    ).map(plan => plan.name -> plan).toMap
