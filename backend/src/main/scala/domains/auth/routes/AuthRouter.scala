package domains.auth.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.*
import org.http4s.HttpRoutes

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        GetSession,
        Logout(sessionStore),
        Login(sessionStore),
        Register(sessionStore),
        UpdateAccount(sessionStore),
        UpdateAccountPermissions,
        DeleteAccount,
        ResolveAccountUsername
      )
    )
