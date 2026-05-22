package domains.auth.http

import domains.auth.http.response.AuthHttpResponses



object AuthHttpPlanDefinitions:

  import AuthHttpPlanRegistry.RegisteredPlan.{
    AuthenticatedPlain,
    AuthenticatedWithTransaction,
    PublicPlain,
    PublicWithTransaction,
    SiteManagerPlain,
    SiteManagerWithTransaction
  }

  val session = AuthenticatedPlain(AuthHttpPlans.Session, AuthHttpResponses.sessionResponse)
  val logout = PublicPlain(AuthHttpPlans.Logout, AuthHttpResponses.loggedOutResponse)
  val listJudgers = SiteManagerPlain(AuthHttpPlans.ListJudgers, AuthHttpResponses.listJudgersResponse)
  val login = PublicWithTransaction(AuthHttpPlans.Login, AuthHttpResponses.loginResponse)
  val register = PublicWithTransaction(AuthHttpPlans.Register, AuthHttpResponses.registerResponse)

  val plans: Map[String, AuthHttpPlanRegistry.RegisteredPlan] =
    List(
      session,
      logout,
      listJudgers,
      login,
      register
    ).map(plan => plan.name -> plan).toMap
