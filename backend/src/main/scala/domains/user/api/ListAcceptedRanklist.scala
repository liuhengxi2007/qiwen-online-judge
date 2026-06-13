package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.utils.UserApiRules

import domains.user.objects.response.UserAcceptedRanklistItem
import domains.user.table.user_profile.UserProfileQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 用户 AC 题数排行榜 API，使用固定页大小返回分页结果。 */
object ListAcceptedRanklist extends AuthenticatedApi[PageRequest, PageResponse[UserAcceptedRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/ranklists/accepted-problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[UserAcceptedRanklistItem]] = summon[Encoder[PageResponse[UserAcceptedRanklistItem]]]

  /** 从查询参数读取 page，非法值回退到第一页。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    /** FIXME-CN: page 非数字时会静默回退第一页，客户端无法知道分页参数被丢弃。 */
    IO.pure(PageRequest(page = request.uri.query.params.get("page").flatMap(_.toIntOption).getOrElse(1)))

  /** 按固定榜单页大小查询 AC 题数排行榜，当前不按 actor 做额外过滤。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, pageRequest: PageRequest): IO[PageResponse[UserAcceptedRanklistItem]] =
    val _ = actor
    UserProfileQueryTable.listAcceptedRanklist(connection, PageRequest(page = pageRequest.page, pageSize = UserApiRules.ranklistPageSize))
