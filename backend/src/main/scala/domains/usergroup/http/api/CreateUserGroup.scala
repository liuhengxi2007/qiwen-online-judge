package domains.usergroup.http.api



import domains.usergroup.http.*
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import cats.effect.IO
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.application.input.{CreateUserGroupRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateUserGroup:

  def routes(context: UserGroupHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "user-groups" =>
        context.handlers.executeDecoded[CreateUserGroupRequest, CreateUserGroupRequest, UserGroupCommands.CreateUserGroupResult](
          request,
          UserGroupHttpPlanDefinitions.createUserGroup
        )(identity)
    }