package domains.rating.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.rating.objects.response.RatingManageState
import domains.rating.table.rating.RatingTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 从评分序列移除最后一场比赛的站点管理员 API，会重算当前评分状态。 */
object PopRatingContest extends SiteManagerApi[Unit, RatingManageState]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/ratings/manage/contests/pop")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RatingManageState] = summon[Encoder[RatingManageState]]

  /** 弹出最后评分比赛不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 删除评分序列尾部比赛；空序列返回业务错误，成功后返回最新管理状态。 */
  override def plan(connection: Connection, actor: SiteManagerUser, input: Unit): IO[RatingManageState] =
    val _ = (actor, input)
    for
      deleted <- RatingTable.popLatestContest(connection)
      _ <- HttpApiError.ensure(deleted, HttpApiError.badRequest(ApiMessages.ratingSequenceEmpty))
      contests <- RatingTable.listManageContests(connection)
    yield RatingManageState(contests)
