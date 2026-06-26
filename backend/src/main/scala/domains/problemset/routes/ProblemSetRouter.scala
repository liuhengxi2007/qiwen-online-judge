package domains.problemset.routes



import cats.effect.IO
import database.DatabaseSession
import domains.problemset.api.ListProblemSets
import domains.problemset.api.GetProblemSet
import domains.problemset.api.CreateProblemSet
import domains.problemset.api.AddProblemToProblemSet
import domains.problemset.api.UpdateProblemSet
import domains.problemset.api.DeleteProblemSet
import domains.problemset.api.UnlinkProblemFromProblemSet
import domains.problemset.api.ResolveProblemSetSlug
import domains.auth.api.SessionStoreContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

/** 汇总题单 domain 的 http4s 路由，统一注入数据库会话和登录会话解析。 */
object ProblemSetRouter:

  /** 构造题单相关 HTTP 路由，包含公开列表、管理入口和内部 slug 解析入口。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStoreContext): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val apiObjectContext = ApiObjectContext(databaseSession, sessionStore)

    ApiObjectRouter.routes(
      apiObjectContext,
      List(
        ListProblemSets,
        CreateProblemSet,
        AddProblemToProblemSet,
        UnlinkProblemFromProblemSet,
        GetProblemSet,
        UpdateProblemSet,
        DeleteProblemSet,
        ResolveProblemSetSlug
      )
    )
