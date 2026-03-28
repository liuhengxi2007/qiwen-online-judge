package routes

import auth.PasswordHasher
import cats.effect.IO
import database.DatabaseSession
import io.circe.syntax.*
import objects.{ErrorResponse, LoginRequest, LoginResponse}
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tables.AuthUserTable

object AuthRouter:

  private val logger = Slf4jLogger.getLogger[IO]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ POST -> Root / "api" / "auth" / "login" =>
      for
        loginRequest <- request.as[LoginRequest]
        _ <- logger.info(s"AuthRouter received login request for ${loginRequest.username}")
        user <- DatabaseSession.withTransactionConnection(connection =>
          AuthUserTable.findByUsername(connection, loginRequest.username)
        )
        response <- user match
          case Some(foundUser) if PasswordHasher.verifyPassword(loginRequest.password, foundUser.passwordHash) =>
            Ok(
              LoginResponse(
                displayName = foundUser.displayName,
                username = foundUser.username,
                message = "Login successful"
              ).asJson
            )
          case _ =>
            IO.pure(
              Response[IO](status = Status.Unauthorized)
                .withEntity(ErrorResponse("Invalid username or password.").asJson)
            )
      yield response
  }
