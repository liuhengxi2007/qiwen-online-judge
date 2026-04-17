package domains.submission.http

import cats.effect.IO
import domains.shared.http.HttpResponseSupport.{errorResponse, validationErrorResponse}
import domains.submission.application.SubmissionCommands
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object SubmissionHttpResponses:

  private def hiddenSubmissionResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, "Submission not found.")

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.HttpResponseSupport.validationErrorResponse(message)

  def mapListResult(result: SubmissionCommands.ListSubmissionsResult): IO[Response[IO]] =
    result match
      case SubmissionCommands.ListSubmissionsResult.Listed(submissions) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(submissions.asJson))

  def mapCreateResult(result: SubmissionCommands.CreateSubmissionResult): IO[Response[IO]] =
    result match
      case SubmissionCommands.CreateSubmissionResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case SubmissionCommands.CreateSubmissionResult.ProblemNotFound =>
        errorResponse(Status.NotFound, "Problem not found.")
      case SubmissionCommands.CreateSubmissionResult.Forbidden =>
        errorResponse(Status.NotFound, "Problem not found.")
      case SubmissionCommands.CreateSubmissionResult.Created(submission) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(submission.asJson))

  def mapGetResult(result: SubmissionCommands.GetSubmissionResult): IO[Response[IO]] =
    result match
      case SubmissionCommands.GetSubmissionResult.NotFound =>
        hiddenSubmissionResponse
      case SubmissionCommands.GetSubmissionResult.Forbidden =>
        hiddenSubmissionResponse
      case SubmissionCommands.GetSubmissionResult.Found(submission) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(submission.asJson))
