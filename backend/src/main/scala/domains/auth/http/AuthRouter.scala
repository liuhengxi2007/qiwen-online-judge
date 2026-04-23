package domains.auth.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.judge.application.JudgeConfig
import domains.auth.model.{LoginRequest, RegisterRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, judgeConfig: JudgeConfig): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthHttpHandlers(databaseSession, sessionStore, judgeConfig)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "auth" / "session" =>
        handlers.execute(request, (), AuthHttpPlanDefinitions.session)

      case request @ POST -> Root / "api" / "auth" / "logout" =>
        handlers.execute(request, AuthHttpSessionSupport.currentSessionToken(request), AuthHttpPlanDefinitions.logout)

      case request @ GET -> Root / "api" / "auth" / "judgers" =>
        handlers.execute(request, (), AuthHttpPlanDefinitions.listJudgers)

      case request @ POST -> Root / "api" / "auth" / "login" =>
        handlers.executeDecoded[LoginRequest, LoginRequest, AuthHttpPlans.LoginOutput](
          request,
          AuthHttpPlanDefinitions.login
        )(identity)

      case request @ POST -> Root / "api" / "auth" / "register" =>
        handlers.executeDecoded[RegisterRequest, RegisterRequest, AuthHttpPlans.RegisterOutput](
          request,
          AuthHttpPlanDefinitions.register
        )(identity)
    }
