package domains.usergroup.http.api



import domains.usergroup.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.usergroup.application.UserGroupCommands
import domains.auth.model.Username
import shared.http.AuthenticatedHttpExecutor
import domains.usergroup.application.input.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest}
import domains.usergroup.model.{UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListUserGroups:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "user-groups" =>
        handlers.execute(request, parsePageRequest(request.uri.query.params), UserGroupHttpPlanDefinitions.listUserGroups)
    }

  private def parsePageRequest(queryParams: Map[String, String]): shared.model.PageRequest =
    shared.model.PageRequest(
      page = parsePositiveInt(queryParams.get("page"), 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)

