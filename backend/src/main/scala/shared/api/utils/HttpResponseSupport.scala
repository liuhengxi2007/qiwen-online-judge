package shared.api.utils



import shared.api.ApiMessage
import cats.effect.IO
import shared.objects.transport.{ErrorResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

/** 统一构造 JSON HTTP 响应的工具，供 API 路由错误处理和简单成功响应复用。 */
object HttpResponseSupport:

  /** 使用可本地化 ApiMessage 构造错误响应。 */
  def errorResponse(status: Status, apiMessage: ApiMessage): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params).asJson))

  /** 使用兜底文本构造错误响应。 */
  def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(ErrorResponse(code = None, message = Some(message), params = Map.empty).asJson))

  /** 使用可本地化 ApiMessage 构造成功响应。 */
  def successResponse(status: Status, apiMessage: ApiMessage): IO[Response[IO]] =
    IO.pure(Response[IO](status = status).withEntity(SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params).asJson))

  /** 构造输入校验失败响应，固定返回 400。 */
  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)
