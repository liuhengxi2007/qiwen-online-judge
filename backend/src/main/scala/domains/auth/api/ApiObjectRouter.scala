package domains.auth.api

import cats.data.OptionT
import cats.effect.IO
import org.http4s.{HttpRoutes, InvalidMessageBodyFailure, MessageBodyFailure, Request, Response, Status}
import shared.api.HttpApiError
import shared.api.utils.HttpResponseSupport

/** 将声明式 ApiObject 列表转换为 http4s 路由，并统一处理 API 错误。 */
object ApiObjectRouter:

  /** 按方法和路径匹配 API 对象，匹配成功后交给对象自身 handle 执行。 */
  def routes(context: ApiObjectContext, apiObjects: List[ApiObject]): HttpRoutes[IO] =
    HttpRoutes { request =>
      matchedApi(request, apiObjects) match
        case Some((apiObject, pathParams)) =>
          OptionT.liftF(handleErrors(apiObject.handle(context, request, pathParams)))
        case None =>
          OptionT.none
    }

  /** 在声明式 API 列表中按 HTTP 方法和路径模板寻找第一个匹配项，并返回解析出的路径参数。 */
  private def matchedApi(
    request: Request[IO],
    apiObjects: List[ApiObject]
  ) =
    apiObjects.iterator
      .filter(_.method == request.method)
      .flatMap(apiObject => apiObject.path.matchParams(request.uri.path.renderString).map(pathParams => (apiObject, pathParams)))
      .toSeq
      .headOption

  /** 将 ApiObject 抛出的受控错误统一转换为 HTTP 响应，避免每个端点重复错误边界。 */
  private def handleErrors(action: IO[Response[IO]]): IO[Response[IO]] =
    action.handleErrorWith {
      case error: HttpApiError =>
        error.toResponse
      case error: InvalidMessageBodyFailure =>
        HttpResponseSupport.errorResponse(Status.BadRequest, error.getMessage)
      case error: MessageBodyFailure =>
        HttpResponseSupport.errorResponse(Status.BadRequest, error.getMessage)
      case error =>
        // FIXME-CN: 非预期异常直接把 error.getMessage 返回给客户端，可能泄露内部实现或数据库错误细节。
        HttpResponseSupport.errorResponse(Status.InternalServerError, error.getMessage)
    }
