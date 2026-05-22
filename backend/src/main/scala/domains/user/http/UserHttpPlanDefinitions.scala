package domains.user.http



import domains.user.http.UserHttpPlanRegistry.RegisteredPlan.{AuthenticatedPlain, AuthenticatedWithTransaction, SiteManagerPlain, SiteManagerWithTransaction}

object UserHttpPlanDefinitions:

  val listUsers = SiteManagerPlain(UserHttpPlans.ListUsers, UserHttpResponses.listUsersResponse)
  val listUserSuggestions = AuthenticatedPlain(UserHttpPlans.ListUserSuggestions, UserHttpResponses.listUserSuggestionsResponse)
  val getUserProfile = AuthenticatedPlain(UserHttpPlans.GetUserProfile, UserHttpResponses.mapGetUserProfileResult)
  val getUserSettings = AuthenticatedPlain(UserHttpPlans.GetUserSettings, UserHttpResponses.mapGetUserSettingsResult)
  val listContributionRanklist = AuthenticatedPlain(UserHttpPlans.ListContributionRanklist, UserHttpResponses.listContributionRanklistResponse)
  val listAcceptedRanklist = AuthenticatedPlain(UserHttpPlans.ListAcceptedRanklist, UserHttpResponses.listAcceptedRanklistResponse)
  val updateUserPermissions = SiteManagerWithTransaction(UserHttpPlans.UpdateUserPermissions, UserHttpResponses.mapUpdateUserPermissionsResult)
  val updateOwnProfile = AuthenticatedWithTransaction(UserHttpPlans.UpdateOwnProfile, UserHttpResponses.mapUpdateUserSettingsOutput)
  val updateOwnPreferences = AuthenticatedWithTransaction(UserHttpPlans.UpdateOwnPreferences, UserHttpResponses.mapUpdateUserSettingsOutput)
  val updateOwnAccount = AuthenticatedWithTransaction(UserHttpPlans.UpdateOwnAccount, UserHttpResponses.mapUpdateUserSettingsOutput)
  val updateManagedProfile = SiteManagerWithTransaction(UserHttpPlans.UpdateManagedProfile, UserHttpResponses.mapUpdateUserSettingsOutput)
  val updateManagedPreferences = SiteManagerWithTransaction(UserHttpPlans.UpdateManagedPreferences, UserHttpResponses.mapUpdateUserSettingsOutput)
  val updateManagedAccount = SiteManagerWithTransaction(UserHttpPlans.UpdateManagedAccount, UserHttpResponses.mapUpdateUserSettingsOutput)
  val deleteUser = SiteManagerWithTransaction(UserHttpPlans.DeleteUser, UserHttpResponses.mapDeleteUserResult)

  val plans: Map[String, UserHttpPlanRegistry.RegisteredPlan] =
    List(
      listUsers,
      listUserSuggestions,
      getUserProfile,
      getUserSettings,
      listContributionRanklist,
      listAcceptedRanklist,
      updateUserPermissions,
      updateOwnProfile,
      updateOwnPreferences,
      updateOwnAccount,
      updateManagedProfile,
      updateManagedPreferences,
      updateManagedAccount,
      deleteUser
    ).map(plan => plan.name -> plan).toMap
