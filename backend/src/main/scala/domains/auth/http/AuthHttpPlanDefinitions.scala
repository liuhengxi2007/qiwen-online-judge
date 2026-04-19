package domains.auth.http

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
  val listUsers = SiteManagerPlain(AuthHttpPlans.ListUsers, AuthHttpResponses.listUsersResponse)
  val listJudgers = SiteManagerPlain(AuthHttpPlans.ListJudgers, AuthHttpResponses.listJudgersResponse)
  val getUserSettings = AuthenticatedPlain(AuthHttpPlans.GetUserSettings, AuthHttpResponses.mapGetUserSettingsResult)
  val getUserProfile = AuthenticatedPlain(AuthHttpPlans.GetUserProfile, AuthHttpResponses.mapGetUserProfileResult)
  val listContributionRanklist =
    AuthenticatedPlain(AuthHttpPlans.ListContributionRanklist, AuthHttpResponses.listContributionRanklistResponse)
  val listAcceptedRanklist =
    AuthenticatedPlain(AuthHttpPlans.ListAcceptedRanklist, AuthHttpResponses.listAcceptedRanklistResponse)
  val updateUserPermissions =
    SiteManagerWithTransaction(AuthHttpPlans.UpdateUserPermissions, AuthHttpResponses.mapUpdateUserPermissionsResult)
  val updateUserSettings =
    AuthenticatedWithTransaction(AuthHttpPlans.UpdateUserSettings, AuthHttpResponses.updateUserSettingsResponse)
  val deleteUser = SiteManagerWithTransaction(AuthHttpPlans.DeleteUser, AuthHttpResponses.mapDeleteUserResult)
  val login = PublicWithTransaction(AuthHttpPlans.Login, AuthHttpResponses.loginResponse)
  val register = PublicWithTransaction(AuthHttpPlans.Register, AuthHttpResponses.registerResponse)

  val plans: Map[String, AuthHttpPlanRegistry.RegisteredPlan] =
    List(
      session,
      logout,
      listUsers,
      listJudgers,
      getUserSettings,
      getUserProfile,
      listContributionRanklist,
      listAcceptedRanklist,
      updateUserPermissions,
      updateUserSettings,
      deleteUser,
      login,
      register
    ).map(plan => plan.name -> plan).toMap
