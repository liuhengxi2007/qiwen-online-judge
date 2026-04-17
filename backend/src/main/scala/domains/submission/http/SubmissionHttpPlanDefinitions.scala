package domains.submission.http

import domains.shared.http.AuthenticatedHttpPlanRegistry

object SubmissionHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listSubmissions = Plain(SubmissionHttpPlans.ListSubmissions, SubmissionHttpResponses.mapListResult)
  val createSubmission = WithTransaction(SubmissionHttpPlans.CreateSubmission, SubmissionHttpResponses.mapCreateResult)
  val getSubmission = Plain(SubmissionHttpPlans.GetSubmission, SubmissionHttpResponses.mapGetResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listSubmissions,
      createSubmission,
      getSubmission
    ).map(plan => plan.name -> plan).toMap
