package routes

import auth.SessionStore
import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*

object ApiRouter:

  def httpApp(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpApp[IO] =
    val allRoutes: HttpRoutes[IO] =
      HealthRouter.routes <+> AuthRouter.routes(databaseSession, sessionStore) <+> PlannerRouter.routes(databaseSession)

    allRoutes.orNotFound
