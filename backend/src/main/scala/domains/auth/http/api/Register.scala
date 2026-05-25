package domains.auth.http.api



import domains.auth.http.*
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.http.mapper.AuthHttpRequestMappers
import cats.effect.IO
import domains.auth.application.AuthCommandResults.RegisterResult
import domains.auth.model.request.RegisterRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object Register:

  def routes(context: AuthHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "auth" / "register" =>
        context.handlers.executeDecoded[RegisterRequest, RegisterRequest, RegisterResult](
          request,
          AuthHttpPlanDefinitions.register
        )(AuthHttpRequestMappers.registerRequest)
    }
