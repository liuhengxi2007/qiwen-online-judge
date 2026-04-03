package domains.problem.http

import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.model.{Problem, ProblemDetail, ProblemListItem, ProblemSummary}
import domains.shared.model.{ErrorResponse, PageResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object ProblemHttpResponses:

  def toProblemListResponse(response: PageResponse[ProblemSummary]): PageResponse[ProblemListItem] =
    response.copy(items = response.items.map(toProblemListItem))

  def toProblemListItem(problem: ProblemSummary): ProblemListItem =
    ProblemListItem(
      id = problem.id,
      slug = problem.slug,
      title = problem.title,
      visibility = problem.visibility,
      status = problem.status,
      ownerUsername = problem.ownerUsername,
      createdAt = problem.createdAt,
      updatedAt = problem.updatedAt
    )

  def toProblemDetail(problem: Problem): ProblemDetail =
    ProblemDetail(
      id = problem.id,
      slug = problem.slug,
      title = problem.title,
      statement = problem.statement,
      visibility = problem.visibility,
      status = problem.status,
      ownerUsername = problem.ownerUsername,
      createdAt = problem.createdAt,
      updatedAt = problem.updatedAt
    )

  def mapCreateResult(result: ProblemCommands.CreateProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.CreateProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.CreateProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.CreateProblemResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, "Problem slug already exists.")
      case ProblemCommands.CreateProblemResult.Created(problem) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(toProblemDetail(problem).asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))

  def mapGetResult(result: ProblemCommands.GetProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.GetProblemResult.NotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.GetProblemResult.Found(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemDetail(problem).asJson))

  def mapUpdateResult(result: ProblemCommands.UpdateProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.UpdateProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.UpdateProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.UpdateProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.UpdateProblemResult.Updated(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemDetail(problem).asJson))

  def mapDeleteResult(result: ProblemCommands.DeleteProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.DeleteProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.DeleteProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.DeleteProblemResult.Deleted =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Problem deleted.").asJson))
