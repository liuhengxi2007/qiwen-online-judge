package routes

import cats.effect.IO
import io.circe.syntax.*
import objects.{ErrorResponse, LoginRequest, LoginResponse}
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

object AuthRouter:

  private val logger = Slf4jLogger.getLogger[IO]
  private val demoEmail = "admin@example.com"
  private val demoPassword = "password123"

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ POST -> Root / "api" / "auth" / "login" =>
      for
        loginRequest <- request.as[LoginRequest]
        _ <- logger.info(s"AuthRouter received login request for ${loginRequest.email}")
        response <-
          if loginRequest.email.trim.equalsIgnoreCase(demoEmail) && loginRequest.password == demoPassword then
            Ok(
              LoginResponse(
                displayName = "Admin User",
                email = demoEmail,
                message = "Login successful"
              ).asJson
            )
          else
            IO.pure(
              Response[IO](status = Status.Unauthorized)
                .withEntity(ErrorResponse("Invalid email or password.").asJson)
            )
      yield response
  }
