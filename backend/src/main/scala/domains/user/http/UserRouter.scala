package domains.user.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.user.http.api.*
import org.http4s.HttpRoutes

object UserRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListUsers,
        ListUserSuggestions,
        GetUserProfile,
        GetUserSettings,
        ListContributionRanklist,
        ListAcceptedRanklist,
        UpdateUserProfile,
        UpdateUserPreferences
      )
    )
