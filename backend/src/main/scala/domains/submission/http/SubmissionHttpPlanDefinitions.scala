package domains.submission.http

object SubmissionHttpPlanDefinitions:

  import SubmissionHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listSubmissions = Plain(SubmissionHttpPlans.ListSubmissions, SubmissionHttpResponses.mapListResult)
  val createSubmission = WithTransaction(SubmissionHttpPlans.CreateSubmission, SubmissionHttpResponses.mapCreateResult)
  val getSubmission = Plain(SubmissionHttpPlans.GetSubmission, SubmissionHttpResponses.mapGetResult)

  val plans: Map[String, SubmissionHttpPlanRegistry.RegisteredPlan] =
    List(
      listSubmissions,
      createSubmission,
      getSubmission
    ).map(plan => plan.name -> plan).toMap
