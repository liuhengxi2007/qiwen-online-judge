package domains.shared.http

import cats.effect.IO
import domains.shared.model.ErrorResponse
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object HttpResponseSupport:

  def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(message).asJson))

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)
