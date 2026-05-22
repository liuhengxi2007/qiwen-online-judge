package domains.user.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.user.http.api.ListUsers
import domains.user.http.api.ListUserSuggestions
import domains.user.http.api.GetUserProfile
import domains.user.http.api.GetUserSettings
import domains.user.http.api.ListContributionRanklist
import domains.user.http.api.ListAcceptedRanklist
import domains.user.http.api.UpdateUserPermissions
import domains.user.http.api.UpdateUserProfile
import domains.user.http.api.UpdateUserPreferences
import domains.user.http.api.UpdateUserAccount
import domains.user.http.api.DeleteUser
import domains.auth.application.SessionStore
import org.http4s.HttpRoutes

object UserRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    ListUsers.routes(databaseSession, sessionStore) <+>
      ListUserSuggestions.routes(databaseSession, sessionStore) <+>
      GetUserProfile.routes(databaseSession, sessionStore) <+>
      GetUserSettings.routes(databaseSession, sessionStore) <+>
      ListContributionRanklist.routes(databaseSession, sessionStore) <+>
      ListAcceptedRanklist.routes(databaseSession, sessionStore) <+>
      UpdateUserPermissions.routes(databaseSession, sessionStore) <+>
      UpdateUserProfile.routes(databaseSession, sessionStore) <+>
      UpdateUserPreferences.routes(databaseSession, sessionStore) <+>
      UpdateUserAccount.routes(databaseSession, sessionStore) <+>
      DeleteUser.routes(databaseSession, sessionStore)
