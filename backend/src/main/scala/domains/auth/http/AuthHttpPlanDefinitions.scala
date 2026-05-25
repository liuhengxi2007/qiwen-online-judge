package domains.auth.http

import domains.auth.http.mapper.AuthHttpResponseMappers



object AuthHttpPlanDefinitions:

  import AuthHttpPlanRegistry.RegisteredPlan.{
    AuthenticatedPlain,
    PublicPlain,
    PublicWithTransaction
  }

  val session = AuthenticatedPlain(AuthHttpPlans.Session, AuthHttpResponseMappers.sessionResponse)
  val logout = PublicPlain(AuthHttpPlans.Logout, AuthHttpResponseMappers.loggedOutResponse)
  val login = PublicWithTransaction(AuthHttpPlans.Login, AuthHttpResponseMappers.loginResponse)
  val register = PublicWithTransaction(AuthHttpPlans.Register, AuthHttpResponseMappers.registerResponse)

  val plans: Map[String, AuthHttpPlanRegistry.RegisteredPlan] =
    List(
      session,
      logout,
      login,
      register
    ).map(plan => plan.name -> plan).toMap
