package shared.http.utils



import shared.http.ApiMessage
import cats.effect.IO
import shared.http.codec.SharedHttpCodecs.given
import shared.objects.response.{ErrorResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object HttpResponseSupport:

  def errorResponse(status: Status, apiMessage: ApiMessage): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params).asJson))

  def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(code = None, message = Some(message), params = Map.empty).asJson))

  def successResponse(status: Status, apiMessage: ApiMessage): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params).asJson))

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)
