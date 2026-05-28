package shared.http

import cats.effect.IO
import org.http4s.Status

final case class HttpApiError(
  status: Status,
  apiMessage: Option[ApiMessage],
  fallbackMessage: Option[String]
) extends RuntimeException(apiMessage.map(_.code).orElse(fallbackMessage).getOrElse(status.reason)):

  def toResponse: IO[org.http4s.Response[IO]] =
    apiMessage match
      case Some(message) =>
        shared.http.utils.HttpResponseSupport.errorResponse(status, message)
      case None =>
        shared.http.utils.HttpResponseSupport.errorResponse(status, fallbackMessage.getOrElse(status.reason))

object HttpApiError:

  def badRequest(message: String): HttpApiError =
    HttpApiError(Status.BadRequest, None, Some(message))

  def badRequest(message: ApiMessage): HttpApiError =
    HttpApiError(Status.BadRequest, Some(message), None)

  def unauthorized(message: ApiMessage): HttpApiError =
    HttpApiError(Status.Unauthorized, Some(message), None)

  def forbidden(message: ApiMessage): HttpApiError =
    HttpApiError(Status.Forbidden, Some(message), None)

  def notFound(message: ApiMessage): HttpApiError =
    HttpApiError(Status.NotFound, Some(message), None)

  def conflict(message: ApiMessage): HttpApiError =
    HttpApiError(Status.Conflict, Some(message), None)

  def internal(message: String): HttpApiError =
    HttpApiError(Status.InternalServerError, None, Some(message))

  def raise[A](error: HttpApiError): IO[A] =
    IO.raiseError(error)

  def ensure(condition: Boolean, error: => HttpApiError): IO[Unit] =
    if condition then IO.unit else raise(error)

  def fromEitherBadRequest[A](value: Either[String, A]): IO[A] =
    IO.fromEither(value.left.map(badRequest))
