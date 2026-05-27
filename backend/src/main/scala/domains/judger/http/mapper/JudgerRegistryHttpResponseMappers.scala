package domains.judger.http.mapper



import cats.effect.IO
import domains.judger.application.JudgerRegistryCommands
import domains.judger.objects.response.RegisteredJudgerListItem
import domains.judger.http.codec.JudgerRegistryHttpCodecs.given
import shared.http.ApiMessages
import shared.http.utils.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object JudgerRegistryHttpResponseMappers:

  def validationErrorResponse(message: String): IO[Response[IO]] =
    shared.http.utils.HttpResponseSupport.validationErrorResponse(message)

  def unauthorizedResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, ApiMessages.judgeTokenInvalid)

  def mapRegisterResult(result: JudgerRegistryCommands.RegisterResult): IO[Response[IO]] =
    result match
      case JudgerRegistryCommands.RegisterResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case JudgerRegistryCommands.RegisterResult.Registered(response) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapHeartbeatResult(result: JudgerRegistryCommands.HeartbeatResult): IO[Response[IO]] =
    result match
      case JudgerRegistryCommands.HeartbeatResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case JudgerRegistryCommands.HeartbeatResult.JudgerNotFound =>
        errorResponse(Status.NotFound, ApiMessages.judgerNotFoundOrExpired)
      case JudgerRegistryCommands.HeartbeatResult.Updated =>
        successResponse(Status.Ok, ApiMessages.judgerHeartbeatRecorded)

  def listRegisteredJudgersResponse(judgers: List[RegisteredJudgerListItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(judgers.asJson))
