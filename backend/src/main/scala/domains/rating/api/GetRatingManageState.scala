package domains.rating.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.rating.objects.response.RatingManageState
import domains.rating.table.rating.RatingTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 获取评分管理状态的站点管理员 API，返回已追加比赛序列。 */
object GetRatingManageState extends SiteManagerApi[Unit, RatingManageState]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/ratings/manage")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RatingManageState] = summon[Encoder[RatingManageState]]

  /** 管理状态查询不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 读取评分比赛序列，调用者身份已由 SiteManagerApi 边界保证。 */
  override def plan(connection: Connection, actor: SiteManagerUser, input: Unit): IO[RatingManageState] =
    val _ = (actor, input)
    RatingTable.listManageContests(connection).map(RatingManageState(_))
