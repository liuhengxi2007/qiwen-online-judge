package domains.user.http.api



import domains.user.http.*
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.http.mapper.UserHttpRequestMappers
import cats.effect.IO
import domains.user.model.Username
import domains.user.application.UserMutationCommands
import domains.user.http.UserHttpPlanDefinitions.{updateUserPermissions}
import domains.user.model.request.{UpdateUserPermissionsRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateUserPermissions:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "users" / targetUsername / "permissions" =>
        context.handlers.executeDecoded[UpdateUserPermissionsRequest, (Username, UpdateUserPermissionsRequest), UserMutationCommands.UpdateUserPermissionsResult](
          request,
          updateUserPermissions
        )(body => UserHttpRequestMappers.updateUserPermissionsInput(targetUsername, body))
    }
