package domains.user.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.user.api.*
import domains.user.utils.UserAvatarStorage
import org.http4s.HttpRoutes

/** user 领域路由聚合器，注册资料、设置、榜单、建议和头像 API。 */
object UserRouter:

  /** 构造 user HTTP routes，并注入数据库、会话和头像存储依赖。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, userAvatarStorage: UserAvatarStorage): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListUsers,
        ListUserSuggestions,
        GetUserProfile,
        GetUserSettings,
        CreateUserProfileSettings,
        FindUserProfileSettings,
        ListContributionRanklist,
        ListAcceptedRanklist,
        UpdateUserProfile,
        UpdateUserPreferences,
        GetUserAvatar(userAvatarStorage),
        UploadUserAvatar(userAvatarStorage),
        DeleteUserAvatar(userAvatarStorage)
      )
    )
