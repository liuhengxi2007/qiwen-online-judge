package shared.api

import cats.effect.IO
import org.http4s.Status

/** HTTP API 业务错误，携带状态码、可本地化消息或兜底文本，并可统一转为响应。 */
final case class HttpApiError(
  status: Status,
  apiMessage: Option[ApiMessage],
  fallbackMessage: Option[String]
) extends RuntimeException(apiMessage.map(_.code).orElse(fallbackMessage).getOrElse(status.reason)):

  /** 将异常转换成 http4s 响应，副作用仅为构造 IO 响应实体。 */
  def toResponse: IO[org.http4s.Response[IO]] =
    apiMessage match
      case Some(message) =>
        shared.api.utils.HttpResponseSupport.errorResponse(status, message)
      case None =>
        shared.api.utils.HttpResponseSupport.errorResponse(status, fallbackMessage.getOrElse(status.reason))

/** 提供常用 HTTP 业务错误构造器和 IO 抛错/校验辅助。 */
object HttpApiError:

  /** 构造 400 文本错误，适用于输入格式或字段校验失败。 */
  def badRequest(message: String): HttpApiError =
    HttpApiError(Status.BadRequest, None, Some(message))

  /** 构造 400 可本地化消息错误。 */
  def badRequest(message: ApiMessage): HttpApiError =
    HttpApiError(Status.BadRequest, Some(message), None)

  /** 构造 401 认证失败错误。 */
  def unauthorized(message: ApiMessage): HttpApiError =
    HttpApiError(Status.Unauthorized, Some(message), None)

  /** 构造 403 授权失败错误，调用方需确认不会泄漏受保护资源存在性。 */
  def forbidden(message: ApiMessage): HttpApiError =
    HttpApiError(Status.Forbidden, Some(message), None)

  /** 构造 404 不存在错误，也用于需要隐藏资源存在性的访问拒绝。 */
  def notFound(message: ApiMessage): HttpApiError =
    HttpApiError(Status.NotFound, Some(message), None)

  /** 构造 409 冲突错误，适用于唯一性或外键业务冲突。 */
  def conflict(message: ApiMessage): HttpApiError =
    HttpApiError(Status.Conflict, Some(message), None)

  /** 构造 500 文本错误，表示服务端边界内的非预期失败。 */
  def internal(message: String): HttpApiError =
    HttpApiError(Status.InternalServerError, None, Some(message))

  /** 在 IO 中抛出业务错误，交由统一路由错误处理转换响应。 */
  def raise[A](error: HttpApiError): IO[A] =
    IO.raiseError(error)

  /** 校验业务条件，失败时惰性构造并抛出指定 API 错误。 */
  def ensure(condition: Boolean, error: => HttpApiError): IO[Unit] =
    if condition then IO.unit else raise(error)

  /** 将领域解析结果提升到 IO，解析失败统一映射为 400。 */
  def fromEitherBadRequest[A](value: Either[String, A]): IO[A] =
    IO.fromEither(value.left.map(badRequest))
