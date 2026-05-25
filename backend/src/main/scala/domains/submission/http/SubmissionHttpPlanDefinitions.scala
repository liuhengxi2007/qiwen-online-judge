package domains.submission.http

import domains.submission.http.mapper.SubmissionHttpResponseMappers



import domains.auth.model.AuthUser
import shared.http.AuthenticatedHttpPlanRegistry

object SubmissionHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  private type PlainPlan[Input, Output] = Plain[AuthUser, Input, Output]
  private type TransactionPlan[Input, Output] = WithTransaction[AuthUser, Input, Output]

  val listSubmissions: PlainPlan[domains.submission.model.request.SubmissionListRequest, domains.submission.application.SubmissionCommands.ListSubmissionsResult] =
    Plain(SubmissionHttpPlans.ListSubmissions, SubmissionHttpResponseMappers.mapListResult)
  val createSubmission: TransactionPlan[domains.submission.model.request.CreateSubmissionRequest, domains.submission.application.SubmissionCommands.CreateSubmissionResult] =
    WithTransaction(SubmissionHttpPlans.CreateSubmission, SubmissionHttpResponseMappers.mapCreateResult)
  val getSubmission: PlainPlan[domains.submission.model.SubmissionId, domains.submission.application.SubmissionCommands.GetSubmissionResult] =
    Plain(SubmissionHttpPlans.GetSubmission, SubmissionHttpResponseMappers.mapGetResult)
  val deleteSubmission: TransactionPlan[domains.submission.model.SubmissionId, domains.submission.application.SubmissionCommands.DeleteSubmissionResult] =
    WithTransaction(SubmissionHttpPlans.DeleteSubmission, SubmissionHttpResponseMappers.mapDeleteResult)
  val rejudgeSubmission: TransactionPlan[domains.submission.model.SubmissionId, domains.submission.application.SubmissionCommands.RejudgeSubmissionResult] =
    WithTransaction(SubmissionHttpPlans.RejudgeSubmission, SubmissionHttpResponseMappers.mapRejudgeResult)
