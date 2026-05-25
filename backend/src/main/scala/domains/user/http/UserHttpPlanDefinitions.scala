package domains.user.http

import domains.user.http.mapper.UserHttpResponseMappers



import domains.user.http.UserHttpPlanRegistry.RegisteredPlan.{AuthenticatedPlain, AuthenticatedWithTransaction, SiteManagerPlain, SiteManagerWithTransaction}

object UserHttpPlanDefinitions:

  val listUsers = SiteManagerPlain(UserHttpPlans.ListUsers, UserHttpResponseMappers.listUsersResponse)
  val listUserSuggestions = AuthenticatedPlain(UserHttpPlans.ListUserSuggestions, UserHttpResponseMappers.listUserSuggestionsResponse)
  val getUserProfile = AuthenticatedPlain(UserHttpPlans.GetUserProfile, UserHttpResponseMappers.mapGetUserProfileResult)
  val getUserSettings = AuthenticatedPlain(UserHttpPlans.GetUserSettings, UserHttpResponseMappers.mapGetUserSettingsResult)
  val listContributionRanklist = AuthenticatedPlain(UserHttpPlans.ListContributionRanklist, UserHttpResponseMappers.listContributionRanklistResponse)
  val listAcceptedRanklist = AuthenticatedPlain(UserHttpPlans.ListAcceptedRanklist, UserHttpResponseMappers.listAcceptedRanklistResponse)
  val updateOwnProfile = AuthenticatedWithTransaction(UserHttpPlans.UpdateOwnProfile, UserHttpResponseMappers.mapUpdateUserSettingsResult)
  val updateOwnPreferences = AuthenticatedWithTransaction(UserHttpPlans.UpdateOwnPreferences, UserHttpResponseMappers.mapUpdateUserSettingsResult)
  val updateManagedProfile = SiteManagerWithTransaction(UserHttpPlans.UpdateManagedProfile, UserHttpResponseMappers.mapUpdateUserSettingsResult)
  val updateManagedPreferences = SiteManagerWithTransaction(UserHttpPlans.UpdateManagedPreferences, UserHttpResponseMappers.mapUpdateUserSettingsResult)
