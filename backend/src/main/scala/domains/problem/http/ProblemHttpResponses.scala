package domains.problem.http

import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.application.ProblemDataStorage
import domains.problem.application.ProblemDataStorage.*
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.http.ProblemHttpPlans.DownloadProblemDataOutput
import domains.shared.http.ApiMessages
import domains.shared.http.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import fs2.Stream
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.Header
import org.typelevel.ci.CIString

object ProblemHttpResponses:

  private def hiddenProblemResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, ApiMessages.problemNotFound)

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.HttpResponseSupport.validationErrorResponse(message)

  def listProblemsResponse(
    response: domains.shared.model.PageResponse[domains.problem.model.ProblemSummary]
  ): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def listProblemSuggestionsResponse(
    response: List[domains.problem.model.ProblemSuggestion]
  ): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapCreateResult(result: ProblemCommands.CreateProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.CreateProblemResult.Forbidden =>
        errorResponse(Status.Forbidden, ApiMessages.problemManagerRequired)
      case ProblemCommands.CreateProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.CreateProblemResult.SlugAlreadyExists =>
        errorResponse(Status.Conflict, ApiMessages.problemSlugExists)
      case ProblemCommands.CreateProblemResult.SlugConflictsWithProblemSet =>
        errorResponse(Status.Conflict, ApiMessages.problemSlugConflictsWithProblemSet)
      case ProblemCommands.CreateProblemResult.Created(problem) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(problem.asJson))

  def mapGetResult(result: ProblemCommands.GetProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.GetProblemResult.NotFound =>
        hiddenProblemResponse
      case ProblemCommands.GetProblemResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.GetProblemResult.Found(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def mapUpdateResult(result: ProblemCommands.UpdateProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.UpdateProblemResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.UpdateProblemResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.UpdateProblemResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.UpdateProblemResult.Updated(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def mapDeleteResult(result: ProblemCommands.DeleteProblemResult): IO[Response[IO]] =
    result match
      case ProblemCommands.DeleteProblemResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.DeleteProblemResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.DeleteProblemResult.Deleted =>
        successResponse(Status.Ok, ApiMessages.problemDeleted)

  def mapUpdateDataResult(result: ProblemCommands.UpdateProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.UpdateProblemDataResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.UpdateProblemDataResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.UpdateProblemDataResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.UpdateProblemDataResult.Updated(result) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(result.asJson))

  def mapListDataResult(result: ProblemCommands.ListProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.ListProblemDataResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.ListProblemDataResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.ListProblemDataResult.Listed(response) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapListDataTreeResult(result: ProblemCommands.ListProblemDataTreeResult): IO[Response[IO]] =
    result match
      case ProblemCommands.ListProblemDataTreeResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.ListProblemDataTreeResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.ListProblemDataTreeResult.Listed(response) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapAuthorizeDownloadResult(result: ProblemCommands.AuthorizeProblemDataDownloadResult): IO[Response[IO]] =
    result match
      case ProblemCommands.AuthorizeProblemDataDownloadResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.AuthorizeProblemDataDownloadResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.AuthorizeProblemDataDownloadResult.Authorized =>
        IO.pure(Response[IO](status = Status.Ok))

  def downloadOutputResponse(problemDataStorage: ProblemDataStorage, output: DownloadProblemDataOutput): IO[Response[IO]] =
    output.authorization match
      case ProblemCommands.AuthorizeProblemDataDownloadResult.Authorized =>
        downloadDataResponse(problemDataStorage, output.problemSlug, output.filename)
      case other =>
        mapAuthorizeDownloadResult(other)

  def mapDeleteDataResult(result: ProblemCommands.DeleteProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.DeleteProblemDataResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.DeleteProblemDataResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.DeleteProblemDataResult.DataFileNotFound =>
        errorResponse(Status.NotFound, ApiMessages.problemDataFileNotFound)
      case ProblemCommands.DeleteProblemDataResult.Deleted(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def mapClearDataResult(result: ProblemCommands.ClearProblemDataResult): IO[Response[IO]] =
    result match
      case ProblemCommands.ClearProblemDataResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.ClearProblemDataResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.ClearProblemDataResult.Cleared(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def mapSetReadyResult(result: ProblemCommands.SetProblemReadyResult): IO[Response[IO]] =
    result match
      case ProblemCommands.SetProblemReadyResult.Forbidden =>
        hiddenProblemResponse
      case ProblemCommands.SetProblemReadyResult.ProblemNotFound =>
        hiddenProblemResponse
      case ProblemCommands.SetProblemReadyResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case ProblemCommands.SetProblemReadyResult.Updated(problem) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(problem.asJson))

  def downloadDataResponse(problemDataStorage: ProblemDataStorage, problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Response[IO]] =
    problemDataStorage.readFile(problemSlug, filename).flatMap {
      case None =>
        errorResponse(Status.NotFound, ApiMessages.problemDataFileNotFound)
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

  def downloadDataPathResponse(problemDataStorage: ProblemDataStorage, problemSlug: ProblemSlug, path: ProblemDataPath): IO[Response[IO]] =
    problemDataStorage.readPath(problemSlug, path).flatMap {
      case None =>
        errorResponse(Status.NotFound, ApiMessages.problemDataFileNotFound)
      case Some((storedPath, bytes)) =>
        IO.pure(
          Response[IO](status = Status.Ok)
            .putHeaders(
              Header.Raw(CIString("Content-Type"), "application/octet-stream"),
              Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="${storedPath.fileName}""""),
              Header.Raw(CIString("Content-Length"), bytes.length.toString)
            )
            .withBodyStream(Stream.emits(bytes).covary[IO])
        )
    }
