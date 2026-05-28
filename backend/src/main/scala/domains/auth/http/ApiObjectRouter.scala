package domains.auth.http

import cats.data.OptionT
import cats.effect.IO
import org.http4s.{HttpRoutes, InvalidMessageBodyFailure, MessageBodyFailure, Request, Response, Status}
import shared.http.HttpApiError
import shared.http.utils.HttpResponseSupport

object ApiObjectRouter:

  def routes(context: ApiObjectContext, apiObjects: List[ApiObject]): HttpRoutes[IO] =
    HttpRoutes { request =>
      matchedApi(request, apiObjects) match
        case Some((apiObject, pathParams)) =>
          OptionT.liftF(handleErrors(apiObject.handle(context, request, pathParams)))
        case None =>
          OptionT.none
    }

  private def matchedApi(
    request: Request[IO],
    apiObjects: List[ApiObject]
  ) =
    apiObjects.iterator
      .filter(_.method == request.method)
      .flatMap(apiObject => apiObject.path.matchParams(request.uri.path.renderString).map(pathParams => (apiObject, pathParams)))
      .toSeq
      .headOption

  private def handleErrors(action: IO[Response[IO]]): IO[Response[IO]] =
    action.handleErrorWith {
      case error: HttpApiError =>
        error.toResponse
      case error: InvalidMessageBodyFailure =>
        HttpResponseSupport.errorResponse(Status.BadRequest, error.getMessage)
      case error: MessageBodyFailure =>
        HttpResponseSupport.errorResponse(Status.BadRequest, error.getMessage)
      case error =>
        HttpResponseSupport.errorResponse(Status.InternalServerError, error.getMessage)
    }
