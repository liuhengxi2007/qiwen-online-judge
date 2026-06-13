package domains.rating.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.auth.utils.SessionStore
import domains.rating.api.*
import org.http4s.HttpRoutes

/** 汇总评分 domain 的 http4s 路由，包含公开排行榜和站点管理员评分管理入口。 */
object RatingRouter:

  /** 构造评分相关 HTTP 路由，站点管理员权限由各 SiteManagerApi 自身处理。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val apiObjectContext = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListRatingRanklist,
        GetRatingManageState,
        AppendRatingContest,
        PopRatingContest,
        GetUserRating
      )
    )
