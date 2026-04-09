package domains.judger.http

import cats.effect.IO
import domains.judger.application.JudgerRegistryCommands
import domains.shared.model.{ErrorResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object JudgerRegistryHttpResponses:

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)

  def unauthorizedResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, "Judge token is invalid.")

  def mapRegisterResult(result: JudgerRegistryCommands.RegisterResult): IO[Response[IO]] =
    result match
      case JudgerRegistryCommands.RegisterResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case JudgerRegistryCommands.RegisterResult.Registered(response) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapHeartbeatResult(result: JudgerRegistryCommands.HeartbeatResult): IO[Response[IO]] =
    result match
      case JudgerRegistryCommands.HeartbeatResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case JudgerRegistryCommands.HeartbeatResult.JudgerNotFound =>
        errorResponse(Status.NotFound, "Judger not found or lease expired.")
      case JudgerRegistryCommands.HeartbeatResult.Updated =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Judger heartbeat recorded.").asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))
