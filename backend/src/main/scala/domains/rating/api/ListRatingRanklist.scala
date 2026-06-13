package domains.rating.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.rating.objects.response.RatingRanklistItem
import domains.rating.table.rating.RatingTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 分页读取评分排行榜的认证 API，面向所有已登录用户。 */
object ListRatingRanklist extends AuthenticatedApi[PageRequest, PageResponse[RatingRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/ratings/ranklist")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[RatingRanklistItem]] = summon[Encoder[PageResponse[RatingRanklistItem]]]

  /** 从查询参数解析分页信息，路径参数不参与排行榜入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  /** 读取当前评分状态表并返回分页排行榜，调用者身份仅用于认证边界。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    pageRequest: PageRequest
  ): IO[PageResponse[RatingRanklistItem]] =
    val _ = actor
    RatingTable.listRanklist(connection, pageRequest)
