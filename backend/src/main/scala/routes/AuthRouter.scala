package routes

import auth.{PasswordHasher, UsernameRules}
import cats.effect.IO
import database.DatabaseSession
import io.circe.syntax.*
import objects.{AuthUserListItem, ErrorResponse, LoginRequest, LoginResponse, RegisterRequest}
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tables.AuthUserTable

object AuthRouter:

  private val logger = Slf4jLogger.getLogger[IO]

  private def invalidCredentialsResponse: IO[Response[IO]] =
    IO.pure(
      Response[IO](status = Status.Unauthorized)
        .withEntity(ErrorResponse("Invalid username or password.").asJson)
    )

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "auth" / "users" =>
      for
        _ <- logger.info("AuthRouter received user list request")
        users <- DatabaseSession.withTransactionConnection(connection =>
          AuthUserTable.listUsers(connection)
        )
        response <- Ok(users.asJson)
      yield response

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
          case _ => invalidCredentialsResponse
      yield response

    case request @ POST -> Root / "api" / "auth" / "register" =>
      for
        registerRequest <- request.as[RegisterRequest]
        _ <- logger.info(s"AuthRouter received register request for ${registerRequest.username}")
        response <- UsernameRules.validate(registerRequest.username) match
          case Some(validationError) =>
            BadRequest(ErrorResponse(validationError).asJson)
          case None =>
            DatabaseSession.withTransactionConnection { connection =>
              for
                existingUser <- AuthUserTable.findByUsername(connection, registerRequest.username)
                result <- existingUser match
                  case Some(_) =>
                    IO.pure(
                      Response[IO](status = Status.Conflict)
                        .withEntity(ErrorResponse("Username already exists, including case-only variations.").asJson)
                    )
                  case None =>
                    AuthUserTable
                      .insert(
                        connection,
                        username = registerRequest.username,
                        displayName = registerRequest.displayName,
                        email = registerRequest.email,
                        password = registerRequest.password
                      )
                      .flatMap { createdUser =>
                        Created(
                          LoginResponse(
                            displayName = createdUser.displayName,
                            username = createdUser.username,
                            message = "Registration successful"
                          ).asJson
                        )
                      }
              yield result
            }
      yield response
  }
