package domains.submission.http



import domains.shared.http.AuthenticatedHttpPlanRegistry

object SubmissionHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listSubmissions = Plain(SubmissionHttpPlans.ListSubmissions, SubmissionHttpResponses.mapListResult)
  val createSubmission = WithTransaction(SubmissionHttpPlans.CreateSubmission, SubmissionHttpResponses.mapCreateResult)
  val getSubmission = Plain(SubmissionHttpPlans.GetSubmission, SubmissionHttpResponses.mapGetResult)
  val deleteSubmission = WithTransaction(SubmissionHttpPlans.DeleteSubmission, SubmissionHttpResponses.mapDeleteResult)
  val rejudgeSubmission = WithTransaction(SubmissionHttpPlans.RejudgeSubmission, SubmissionHttpResponses.mapRejudgeResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listSubmissions,
      createSubmission,
      getSubmission,
      deleteSubmission,
      rejudgeSubmission
    ).map(plan => plan.name -> plan).toMap
