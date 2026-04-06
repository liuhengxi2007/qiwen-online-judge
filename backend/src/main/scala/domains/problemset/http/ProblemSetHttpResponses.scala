package domains.problemset.http

import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problemset.model.{ProblemSet, ProblemSetDetail, ProblemSetProblem, ProblemSetProblemSummary, ProblemSetSummary, ProblemSetSummaryView}
import domains.shared.model.{ErrorResponse, PageResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object ProblemSetHttpResponses:

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)

  def toProblemSetListResponse(response: PageResponse[ProblemSetSummaryView]): PageResponse[ProblemSetSummary] =
    response.copy(items = response.items.map(toProblemSetSummary))

  def toProblemSetSummary(problemSet: ProblemSetSummaryView): ProblemSetSummary =
    ProblemSetSummary(
      id = problemSet.id,
      slug = problemSet.slug,
      title = problemSet.title,
      description = problemSet.description,
      accessPolicy = problemSet.accessPolicy,
      ownerUsername = problemSet.ownerUsername,
      createdAt = problemSet.createdAt,
      updatedAt = problemSet.updatedAt
    )

  def toProblemSetProblemSummary(problem: ProblemSetProblem): ProblemSetProblemSummary =
    ProblemSetProblemSummary(
      id = problem.id,
      slug = problem.slug,
      title = problem.title,
      position = problem.position
    )

  def toProblemSetDetail(problemSet: ProblemSet): ProblemSetDetail =
    ProblemSetDetail(
      id = problemSet.id,
      slug = problemSet.slug,
      title = problemSet.title,
      description = problemSet.description,
      problems = problemSet.problems.map(toProblemSetProblemSummary),
      accessPolicy = problemSet.accessPolicy,
      ownerUsername = problemSet.ownerUsername,
      createdAt = problemSet.createdAt,
      updatedAt = problemSet.updatedAt
    )

  def mapCreateResult(result: ProblemSetCommands.CreateProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.CreateProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.CreateProblemSetResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.CreateProblemSetResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, "Problem set slug already exists.")
      case ProblemSetCommands.CreateProblemSetResult.SlugConflictsWithProblem =>
        errorResponse(Status.Conflict, "Problem set slug conflicts with an existing problem slug.")
      case ProblemSetCommands.CreateProblemSetResult.Created(problemSet) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(toProblemSetDetail(problemSet).asJson))

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
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))

  def mapGetResult(result: ProblemSetCommands.GetProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.GetProblemSetResult.NotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.GetProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, "You do not have access to this problem set.")
      case ProblemSetCommands.GetProblemSetResult.Found(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))

  def mapUpdateResult(result: ProblemSetCommands.UpdateProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.UpdateProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, "Problem manager permission required.")
      case ProblemSetCommands.UpdateProblemSetResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.UpdateProblemSetResult.ProblemSetNotFound =>
        errorResponse(Status.NotFound, "Problem set not found.")
      case ProblemSetCommands.UpdateProblemSetResult.Updated(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))

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
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))
