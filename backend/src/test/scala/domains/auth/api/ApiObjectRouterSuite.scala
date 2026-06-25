package domains.auth.api

import cats.effect.IO
import database.DatabaseSession
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{InvalidMessageBodyFailure, Method, Request, Response, Status, Uri}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.transport.ErrorResponse

class ApiObjectRouterSuite extends CatsEffectSuite:

  private val context = ApiObjectContext.public(null.asInstanceOf[DatabaseSession])

  test("routes matching method and path to the registered API object") {
    val routes = ApiObjectRouter.routes(context, List(EchoPathParamApi)).orNotFound
    val request = Request[IO](method = Method.GET, uri = uri("/api/items/abc"))

    routes.run(request).flatMap { response =>
      response.as[String].map { body =>
        assertEquals(response.status, Status.Ok)
        assertEquals(body, "abc")
      }
    }
  }

  test("returns 404 when method does not match") {
    val routes = ApiObjectRouter.routes(context, List(EchoPathParamApi)).orNotFound
    val request = Request[IO](method = Method.POST, uri = uri("/api/items/abc"))

    routes.run(request).map(response => assertEquals(response.status, Status.NotFound))
  }

  test("maps request decode failures to bad request responses") {
    val routes = ApiObjectRouter.routes(context, List(DecodeFailureApi)).orNotFound
    val request = Request[IO](method = Method.GET, uri = uri("/api/decode-failure"))

    routes.run(request).map(response => assertEquals(response.status, Status.BadRequest))
  }

  test("maps HttpApiError to code-only error responses") {
    val routes = ApiObjectRouter.routes(context, List(KnownErrorApi)).orNotFound
    val request = Request[IO](method = Method.GET, uri = uri("/api/known-error"))

    routes.run(request).flatMap { response =>
      response.as[ErrorResponse].map { body =>
        assertEquals(response.status, Status.Forbidden)
        assertEquals(body.code, Some(ApiMessages.siteManagerRequired.code))
        assertEquals(body.message, None)
      }
    }
  }

  private object EchoPathParamApi extends ApiObject:
    override val method: Method = Method.GET
    override val path: ApiPath = ApiPath("/api/items/:itemId")

    override private[api] def handle(
      context: ApiObjectContext,
      request: Request[IO],
      pathParams: PathParams
    ): IO[Response[IO]] =
      val _ = (context, request)
      IO.pure(Response[IO](status = Status.Ok).withEntity(pathParams.require("itemId").toOption.getOrElse("")))

  private object DecodeFailureApi extends ApiObject:
    override val method: Method = Method.GET
    override val path: ApiPath = ApiPath("/api/decode-failure")

    override private[api] def handle(
      context: ApiObjectContext,
      request: Request[IO],
      pathParams: PathParams
    ): IO[Response[IO]] =
      val _ = (context, request, pathParams)
      IO.raiseError(InvalidMessageBodyFailure("decode failed"))

  private object KnownErrorApi extends ApiObject:
    override val method: Method = Method.GET
    override val path: ApiPath = ApiPath("/api/known-error")

    override private[api] def handle(
      context: ApiObjectContext,
      request: Request[IO],
      pathParams: PathParams
    ): IO[Response[IO]] =
      val _ = (context, request, pathParams)
      HttpApiError.raise(HttpApiError.forbidden(ApiMessages.siteManagerRequired))

  private def uri(value: String): Uri =
    Uri.fromString(value).fold(error => fail(error.toString), identity)
