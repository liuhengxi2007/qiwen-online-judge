package domains.auth.http.api



import domains.auth.http.*
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.http.mapper.AuthHttpRequestMappers
import cats.effect.IO
import domains.auth.application.AuthCommandResults.LoginResult
import domains.auth.model.request.LoginRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object Login:

  def routes(context: AuthHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "auth" / "login" =>
        context.handlers.executeDecoded[LoginRequest, LoginRequest, LoginResult](
          request,
          AuthHttpPlanDefinitions.login
        )(AuthHttpRequestMappers.loginRequest)
    }
