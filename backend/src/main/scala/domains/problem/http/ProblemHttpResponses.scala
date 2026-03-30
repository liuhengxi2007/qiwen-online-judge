package domains.problem.http

import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.shared.model.{ErrorResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object ProblemHttpResponses:

  def mapCreateResult(result: ProblemCommands.CreateProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.CreateProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.CreateProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.CreateProblemResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, "Problem slug already exists.")
      case ProblemCommands.CreateProblemResult.Created(problem) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(problem.asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))

  def mapGetResult(result: ProblemCommands.GetProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.GetProblemResult.NotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.GetProblemResult.Found(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def mapUpdateResult(result: ProblemCommands.UpdateProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.UpdateProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.UpdateProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.UpdateProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.UpdateProblemResult.Updated(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def mapDeleteResult(result: ProblemCommands.DeleteProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.DeleteProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.DeleteProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.DeleteProblemResult.Deleted =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Problem deleted.").asJson))
