package domains.problemset.http



import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problemset.model.{ProblemSet}
import domains.problemset.application.view.{ProblemSetDetail}
import domains.shared.http.ApiMessages
import domains.shared.http.utils.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object ProblemSetHttpResponses:

  private def hiddenProblemSetResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, ApiMessages.problemSetNotFound)

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.utils.HttpResponseSupport.validationErrorResponse(message)

  def listProblemSetsResponse(response: domains.shared.model.PageResponse[domains.problemset.application.view.ProblemSetSummary]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def toProblemSetDetail(problemSet: ProblemSet): ProblemSetDetail =
    ProblemSetDetail(
      id = problemSet.id,
      slug = problemSet.slug,
      title = problemSet.title,
      description = problemSet.description,
      problems = problemSet.problems,
      accessPolicy = problemSet.accessPolicy,
      creator = problemSet.creator,
      createdAt = problemSet.createdAt,
      updatedAt = problemSet.updatedAt
    )

  def mapCreateResult(result: ProblemSetCommands.CreateProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.CreateProblemSetResult.Forbidden =>
        errorResponse(Status.Forbidden, ApiMessages.problemManagerRequired)
      case ProblemSetCommands.CreateProblemSetResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.CreateProblemSetResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, ApiMessages.problemSetSlugExists)
      case ProblemSetCommands.CreateProblemSetResult.SlugConflictsWithProblem =>
        errorResponse(Status.Conflict, ApiMessages.problemSetSlugConflictsWithProblem)
      case ProblemSetCommands.CreateProblemSetResult.Created(problemSet) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(toProblemSetDetail(problemSet).asJson))

  def mapAddProblemResult(result: ProblemSetCommands.AddProblemResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.AddProblemResult.Forbidden =>
        hiddenProblemSetResponse
      case ProblemSetCommands.AddProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.AddProblemResult.ProblemSetNotFound =>
        errorResponse(Status.NotFound, ApiMessages.problemSetNotFound)
      case ProblemSetCommands.AddProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, ApiMessages.problemNotFound)
      case ProblemSetCommands.AddProblemResult.ProblemAlreadyLinked =>
        errorResponse(Status.Conflict, ApiMessages.problemAlreadyLinkedToProblemSet)
      case ProblemSetCommands.AddProblemResult.Linked(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))

  def mapGetResult(result: ProblemSetCommands.GetProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.GetProblemSetResult.NotFound =>
        hiddenProblemSetResponse
      case ProblemSetCommands.GetProblemSetResult.Forbidden =>
        hiddenProblemSetResponse
      case ProblemSetCommands.GetProblemSetResult.Found(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))

  def mapUpdateResult(result: ProblemSetCommands.UpdateProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.UpdateProblemSetResult.Forbidden =>
        hiddenProblemSetResponse
      case ProblemSetCommands.UpdateProblemSetResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemSetCommands.UpdateProblemSetResult.ProblemSetNotFound =>
        hiddenProblemSetResponse
      case ProblemSetCommands.UpdateProblemSetResult.Updated(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))

  def mapDeleteResult(result: ProblemSetCommands.DeleteProblemSetResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.DeleteProblemSetResult.Forbidden =>
        hiddenProblemSetResponse
      case ProblemSetCommands.DeleteProblemSetResult.ProblemSetNotFound =>
        hiddenProblemSetResponse
      case ProblemSetCommands.DeleteProblemSetResult.Deleted =>
        successResponse(Status.Ok, ApiMessages.problemSetDeleted)

  def mapRemoveProblemResult(result: ProblemSetCommands.RemoveProblemResult): IO[Response[IO]] =
    result match
      case ProblemSetCommands.RemoveProblemResult.Forbidden =>
        hiddenProblemSetResponse
      case ProblemSetCommands.RemoveProblemResult.ProblemSetNotFound =>
        hiddenProblemSetResponse
      case ProblemSetCommands.RemoveProblemResult.ProblemNotFound =>
        errorResponse(Status.NotFound, ApiMessages.problemNotFound)
      case ProblemSetCommands.RemoveProblemResult.ProblemNotLinked =>
        errorResponse(Status.NotFound, ApiMessages.problemNotLinkedToProblemSet)
      case ProblemSetCommands.RemoveProblemResult.Removed(problemSet) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toProblemSetDetail(problemSet).asJson))
