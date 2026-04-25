package domains.user.http

import domains.user.http.UserHttpPlanRegistry.RegisteredPlan.{AuthenticatedPlain, AuthenticatedWithTransaction, SiteManagerPlain, SiteManagerWithTransaction}

object UserHttpPlanDefinitions:

  val listUsers = SiteManagerPlain(UserHttpPlans.ListUsers, UserHttpResponses.listUsersResponse)
  val getUserProfile = AuthenticatedPlain(UserHttpPlans.GetUserProfile, UserHttpResponses.mapGetUserProfileResult)
  val getUserSettings = AuthenticatedPlain(UserHttpPlans.GetUserSettings, UserHttpResponses.mapGetUserSettingsResult)
  val listContributionRanklist = AuthenticatedPlain(UserHttpPlans.ListContributionRanklist, UserHttpResponses.listContributionRanklistResponse)
  val listAcceptedRanklist = AuthenticatedPlain(UserHttpPlans.ListAcceptedRanklist, UserHttpResponses.listAcceptedRanklistResponse)
  val updateUserPermissions = SiteManagerWithTransaction(UserHttpPlans.UpdateUserPermissions, UserHttpResponses.mapUpdateUserPermissionsResult)
  val updateOwnSettings = AuthenticatedWithTransaction(UserHttpPlans.UpdateOwnSettings, UserHttpResponses.mapUpdateUserSettingsOutput)
  val updateManagedSettings = SiteManagerWithTransaction(UserHttpPlans.UpdateManagedSettings, UserHttpResponses.mapUpdateUserSettingsOutput)
  val deleteUser = SiteManagerWithTransaction(UserHttpPlans.DeleteUser, UserHttpResponses.mapDeleteUserResult)

  val plans: Map[String, UserHttpPlanRegistry.RegisteredPlan] =
    List(
      listUsers,
      getUserProfile,
      getUserSettings,
      listContributionRanklist,
      listAcceptedRanklist,
      updateUserPermissions,
      updateOwnSettings,
      updateManagedSettings,
      deleteUser
    ).map(plan => plan.name -> plan).toMap
