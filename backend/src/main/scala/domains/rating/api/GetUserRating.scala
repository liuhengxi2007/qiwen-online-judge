package domains.rating.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.rating.objects.RatingValue
import domains.rating.table.rating.RatingTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部用户评分查询 API，供其他 domain 聚合用户展示信息时复用。 */
object GetUserRating extends InternalOnlyApi[Username, RatingValue]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/ratings/user")

  /** 读取用户当前评分；没有评分状态时返回初始评分。 */
  override def plan(connection: Connection, username: Username): IO[RatingValue] =
    RatingTable.findUserRating(connection, username)
