package domains.auth.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.SessionStoreContext
import domains.auth.api.*
import org.http4s.HttpRoutes

/** auth 领域路由聚合器，注册登录、注册、会话和账号管理 API。 */
object AuthRouter:

  /** 构造 auth HTTP routes，并注入数据库会话和会话存储依赖。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStoreContext): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      context,
      List(
        GetSession,
        Logout(sessionStore),
        Login(sessionStore),
        Register(sessionStore),
        UpdateAccount(sessionStore),
        UpdateAccountPermissions,
        DeleteAccount,
        ResolveAccountUsername
      )
    )
