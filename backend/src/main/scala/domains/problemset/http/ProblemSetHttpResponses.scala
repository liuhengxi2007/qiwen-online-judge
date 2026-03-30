package domains.problemset.http

import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.shared.model.ErrorResponse
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object ProblemSetHttpResponses:

  def mapCreateResult(result: ProblemSetCommands.CreateProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.CreateProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.CreateProblemSetResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.CreateProblemSetResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, "Problem set slug already exists.")
      case ProblemSetCommands.CreateProblemSetResult.Created(problemSet) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(problemSet.asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))

  def mapAddProblemResult(result: ProblemSetCommands.AddProblemResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.AddProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.AddProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.AddProblemResult.ProblemSetNotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.AddProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemSetCommands.AddProblemResult.ProblemAlreadyLinked =>
        errorResponse(Status.Conflict, "Problem is already linked to this problem set.")
      case ProblemSetCommands.AddProblemResult.Linked(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problemSet.asJson))

  def mapGetResult(result: ProblemSetCommands.GetProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.GetProblemSetResult.NotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.GetProblemSetResult.Found(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problemSet.asJson))

  def mapUpdateResult(result: ProblemSetCommands.UpdateProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.UpdateProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.UpdateProblemSetResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.UpdateProblemSetResult.ProblemSetNotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.UpdateProblemSetResult.Updated(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problemSet.asJson))

  def mapDeleteResult(result: ProblemSetCommands.DeleteProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.DeleteProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.DeleteProblemSetResult.ProblemSetNotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.DeleteProblemSetResult.Deleted =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(domains.shared.model.SuccessResponse("Problem set deleted.").asJson))

  def mapRemoveProblemResult(result: ProblemSetCommands.RemoveProblemResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.RemoveProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.RemoveProblemResult.ProblemSetNotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.RemoveProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemSetCommands.RemoveProblemResult.ProblemNotLinked =>
        errorResponse(Status.NotFound, "Problem is not linked to this problem set.")
      case ProblemSetCommands.RemoveProblemResult.Removed(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problemSet.asJson))
