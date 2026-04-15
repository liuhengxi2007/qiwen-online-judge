package routes

import cats.effect.IO
import cats.syntax.semigroupk.*
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.implicits.*

object ApiRouter:

  private val allRoutes: HttpRoutes[IO] =
    HealthRouter.routes <+> PlannerRouter.routes

  val httpApp: HttpApp[IO] = allRoutes.orNotFound
