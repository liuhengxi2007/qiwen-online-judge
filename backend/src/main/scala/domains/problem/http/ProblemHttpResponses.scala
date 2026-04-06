package domains.problem.http

import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.application.ProblemDataStorage
import domains.problem.model.{Problem, ProblemDataFileListResponse, ProblemDataFilename, ProblemDetail, ProblemListItem, ProblemSlug, ProblemSummary}
import domains.shared.model.{ErrorResponse, PageResponse, SuccessResponse}
import fs2.Stream
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.Header
import org.typelevel.ci.CIString

object ProblemHttpResponses:

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)

  def toProblemListResponse(response: PageResponse[ProblemSummary]): PageResponse[ProblemListItem] =
    response.copy(items = response.items.map(toProblemListItem))

  def toProblemListItem(problem: ProblemSummary): ProblemListItem =
    ProblemListItem(
      id = problem.id,
      slug = problem.slug,
      title = problem.title,
      data = problem.data,
      timeLimitMs = problem.timeLimitMs,
      spaceLimitMb = problem.spaceLimitMb,
      accessPolicy = problem.accessPolicy,
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
      data = problem.data,
      timeLimitMs = problem.timeLimitMs,
      spaceLimitMb = problem.spaceLimitMb,
      accessPolicy = problem.accessPolicy,
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
      case ProblemCommands.CreateProblemResult.SlugConflictsWithProblemSet =>
        errorResponse(Status.Conflict, "Problem slug conflicts with an existing problem set slug.")
      case ProblemCommands.CreateProblemResult.Created(problem) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(toProblemDetail(problem).asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))

  def mapGetResult(result: ProblemCommands.GetProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.GetProblemResult.NotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.GetProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, "You do not have access to this problem.")
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

  def mapUpdateDataResult(result: ProblemCommands.UpdateProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.UpdateProblemDataResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.UpdateProblemDataResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.UpdateProblemDataResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.UpdateProblemDataResult.Updated(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemDetail(problem).asJson))

  def mapListDataResult(result: ProblemCommands.ListProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.ListProblemDataResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.ListProblemDataResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.ListProblemDataResult.Listed(response) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapDeleteDataResult(result: ProblemCommands.DeleteProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.DeleteProblemDataResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemCommands.DeleteProblemDataResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case ProblemCommands.DeleteProblemDataResult.DataFileNotFound =>
        errorResponse(Status.NotFound, "Problem data file not found.")
      case ProblemCommands.DeleteProblemDataResult.Deleted(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemDetail(problem).asJson))

  def downloadDataResponse(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Response[IO]] =
    ProblemDataStorage.readFile(problemSlug, filename).flatMap {
      case None =>
        errorResponse(Status.NotFound, "Problem data file not found.")
      case Some((sanitizedFilename, bytes)) =>
        IO.pure(
          Response[IO](status = Status.Ok)
            .putHeaders(
              Header.Raw(CIString("Content-Type"), "application/octet-stream"),
              Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="${sanitizedFilename.value}""""),
              Header.Raw(CIString("Content-Length"), bytes.length.toString)
            )
            .withBodyStream(Stream.emits(bytes).covary[IO])
        )
    }
