package domains.usergroup.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.usergroup.http.api.ListUserGroups
import domains.usergroup.http.api.GetUserGroup
import domains.usergroup.http.api.CreateUserGroup
import domains.usergroup.http.api.UpdateUserGroup
import domains.usergroup.http.api.DeleteUserGroup
import domains.usergroup.http.api.AddUserGroupMember
import domains.usergroup.http.api.UpdateUserGroupMemberRole
import domains.usergroup.http.api.RemoveUserGroupMember
import domains.auth.application.SessionStore
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object UserGroupRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = UserGroupHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    )

    ListUserGroups.routes(context) <+>
      GetUserGroup.routes(context) <+>
      CreateUserGroup.routes(context) <+>
      UpdateUserGroup.routes(context) <+>
      DeleteUserGroup.routes(context) <+>
      AddUserGroupMember.routes(context) <+>
      UpdateUserGroupMemberRole.routes(context) <+>
      RemoveUserGroupMember.routes(context)
