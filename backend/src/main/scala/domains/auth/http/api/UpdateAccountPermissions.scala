package domains.auth.http.api

import cats.effect.IO
import domains.auth.application.AuthCommands
import domains.auth.http.*
import domains.auth.http.AuthHttpPlanDefinitions.updateUserPermissions
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.http.mapper.AuthHttpRequestMappers
import domains.auth.objects.request.UpdateUserPermissionsRequest
import domains.user.objects.Username
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateAccountPermissions:

  def routes(handlers: AuthHttpHandlers)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "auth" / "accounts" / targetUsername / "permissions" =>
        handlers.executeDecoded[UpdateUserPermissionsRequest, (Username, UpdateUserPermissionsRequest), AuthCommands.UpdateUserPermissionsResult](
          request,
          updateUserPermissions
        )(body => AuthHttpRequestMappers.updateUserPermissionsInput(targetUsername, body))
    }
