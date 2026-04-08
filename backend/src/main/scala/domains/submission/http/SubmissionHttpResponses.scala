package domains.submission.http

import cats.effect.IO
import domains.shared.model.ErrorResponse
import domains.submission.application.SubmissionCommands
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object SubmissionHttpResponses:

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)

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
        errorResponse(Status.Forbidden, "You do not have access to submit to this problem.")
      case SubmissionCommands.CreateSubmissionResult.Created(submission) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(submission.asJson))

  def mapGetResult(result: SubmissionCommands.GetSubmissionResult): IO[Response[IO]] =
    result match
      case SubmissionCommands.GetSubmissionResult.NotFound =>
        errorResponse(Status.NotFound, "Submission not found.")
      case SubmissionCommands.GetSubmissionResult.Forbidden =>
        errorResponse(Status.Forbidden, "You do not have access to this submission.")
      case SubmissionCommands.GetSubmissionResult.Found(submission) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(submission.asJson))

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))
