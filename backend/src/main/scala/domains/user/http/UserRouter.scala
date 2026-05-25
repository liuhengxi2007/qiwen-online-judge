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
import org.http4s.dsl.Http4sDsl

object UserRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new UserHttpHandlers(databaseSession, sessionStore)

    val endpointRoutes = List(
      ListUsers.routes(handlers),
      ListUserSuggestions.routes(handlers),
      GetUserProfile.routes(handlers),
      GetUserSettings.routes(handlers),
      ListContributionRanklist.routes(handlers),
      ListAcceptedRanklist.routes(handlers),
      UpdateUserPermissions.routes(handlers),
      UpdateUserProfile.routes(handlers),
      UpdateUserPreferences.routes(handlers),
      UpdateUserAccount.routes(handlers),
      DeleteUser.routes(handlers)
    )

    endpointRoutes.reduce(_ <+> _)
