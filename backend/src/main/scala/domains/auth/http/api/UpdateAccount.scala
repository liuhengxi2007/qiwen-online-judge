package domains.auth.http.api

import cats.effect.IO
import domains.auth.http.*
import domains.auth.http.mapper.AuthHttpRequestMappers
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateAccount:

  def routes(handlers: AuthHttpHandlers)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "auth" / "accounts" / targetUsername / "settings" / "account" =>
        handlers.executeAccountUpdate(request, AuthHttpRequestMappers.username(targetUsername))
    }
