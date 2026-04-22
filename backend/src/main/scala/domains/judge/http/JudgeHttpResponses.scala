package domains.judge.http

import cats.effect.IO
import domains.judge.application.JudgeCommands
import domains.shared.http.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import domains.shared.model.ApiMessages
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object JudgeHttpResponses:

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.HttpResponseSupport.validationErrorResponse(message)

  def unauthorizedResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, ApiMessages.judgeTokenInvalid)

  def noTaskResponse: IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.NoContent))

  def mapClaimResult(result: JudgeCommands.ClaimJudgeTaskResult): IO[Response[IO]] =
    result match
      case JudgeCommands.ClaimJudgeTaskResult.NoTask =>
        noTaskResponse
      case JudgeCommands.ClaimJudgeTaskResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case JudgeCommands.ClaimJudgeTaskResult.Claimed(task) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(task.asJson))

  def mapReportResult(result: JudgeCommands.ReportJudgeResult): IO[Response[IO]] =
    result match
      case JudgeCommands.ReportJudgeResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case JudgeCommands.ReportJudgeResult.SubmissionNotFound =>
        errorResponse(Status.NotFound, ApiMessages.submissionNotFound)
      case JudgeCommands.ReportJudgeResult.Updated =>
        successResponse(Status.Ok, ApiMessages.judgeResultRecorded)
