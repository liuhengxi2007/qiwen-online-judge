package domains.usergroup.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.usergroup.api.*
import org.http4s.HttpRoutes

/** usergroup 领域路由聚合器，注册用户组和成员管理 API。 */
object UserGroupRouter:

  /** 构造 usergroup HTTP routes，并注入数据库和会话依赖。 */
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
        RemoveUserGroupMember,
        ResolveUserGroupSlug,
        ListUserGroupSlugsForMember
      )
    )
