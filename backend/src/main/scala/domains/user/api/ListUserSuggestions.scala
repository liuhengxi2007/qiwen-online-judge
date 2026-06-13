package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.utils.UserApiRules

import domains.user.objects.UserIdentity
import domains.user.objects.request.UserSearchQuery
import domains.user.table.user_profile.UserProfileQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 用户搜索建议 API，供输入框按关键词查找用户身份摘要。 */
object ListUserSuggestions extends AuthenticatedApi[UserSearchQuery, List[UserIdentity]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/suggestions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[UserIdentity]] = summon[Encoder[List[UserIdentity]]]

  /** 从 q 查询参数解析搜索词，缺失或为空返回 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[UserSearchQuery] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(UserSearchQuery.parse(request.uri.query.params.get("q").getOrElse("")))

  /** 搜索词过短时直接返回空列表，否则查询最多固定数量建议。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, query: UserSearchQuery): IO[List[UserIdentity]] =
    val _ = actor
    if query.value.length < UserApiRules.minSuggestionQueryLength then IO.pure(Nil)
    else UserProfileQueryTable.listSuggestions(connection, query)
