package domains.auth.http

import domains.auth.http.mapper.AuthHttpResponseMappers



object AuthHttpPlanDefinitions:

  import AuthHttpPlanRegistry.RegisteredPlan.{
    AuthenticatedPlain,
    AuthenticatedWithTransaction,
    PublicPlain,
    PublicWithTransaction,
    SiteManagerWithTransaction
  }

  val session = AuthenticatedPlain(AuthHttpPlans.Session, AuthHttpResponseMappers.sessionResponse)
  val logout = PublicPlain(AuthHttpPlans.Logout, AuthHttpResponseMappers.loggedOutResponse)
  val login = PublicWithTransaction(AuthHttpPlans.Login, AuthHttpResponseMappers.loginResponse)
  val register = PublicWithTransaction(AuthHttpPlans.Register, AuthHttpResponseMappers.registerResponse)
  val updateUserPermissions = SiteManagerWithTransaction(AuthHttpPlans.UpdateUserPermissions, AuthHttpResponseMappers.mapUpdateUserPermissionsResult)
  val updateOwnAccount = AuthenticatedWithTransaction(AuthHttpPlans.UpdateOwnAccount, AuthHttpResponseMappers.mapUpdateAccountOutput)
  val updateManagedAccount = SiteManagerWithTransaction(AuthHttpPlans.UpdateManagedAccount, AuthHttpResponseMappers.mapUpdateAccountOutput)
  val deleteAccount = SiteManagerWithTransaction(AuthHttpPlans.DeleteAccount, AuthHttpResponseMappers.mapDeleteAccountResult)
