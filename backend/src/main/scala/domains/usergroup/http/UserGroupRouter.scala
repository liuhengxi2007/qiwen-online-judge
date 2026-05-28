package domains.usergroup.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.usergroup.http.api.*
import org.http4s.HttpRoutes

object UserGroupRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListUserGroups,
        GetUserGroup,
        CreateUserGroup,
        UpdateUserGroup,
        DeleteUserGroup,
        AddUserGroupMember,
        UpdateUserGroupMemberRole,
        RemoveUserGroupMember
      )
    )
