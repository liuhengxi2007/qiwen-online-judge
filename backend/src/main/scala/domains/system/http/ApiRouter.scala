package domains.system.http

import domains.auth.application.SessionStore
import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*

object ApiRouter:

  def httpApp(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpApp[IO] =
    val allRoutes: HttpRoutes[IO] =
      domains.system.health.HealthRouter.routes <+> domains.auth.http.AuthRouter.routes(databaseSession, sessionStore) <+> domains.system.planner.http.PlannerRouter.routes(databaseSession)

    allRoutes.orNotFound
