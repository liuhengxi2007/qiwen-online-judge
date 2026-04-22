package domains.shared.http

import cats.effect.IO
import domains.shared.model.{ApiMessage, ErrorResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object HttpResponseSupport:

  def errorResponse(status: Status, apiMessage: ApiMessage): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(apiMessage).asJson))

  def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse.legacy(message).asJson))

  def successResponse(status: Status, apiMessage: ApiMessage): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(SuccessResponse(apiMessage).asJson))

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)
