package domains.user.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.user.http.UserApiSupport
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.objects.UserIdentity
import domains.user.objects.request.UserSearchQuery
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object ListUserSuggestions extends AuthenticatedApi[UserSearchQuery, List[UserIdentity]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/suggestions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[UserIdentity]] = summon[Encoder[List[UserIdentity]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[UserSearchQuery] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(UserSearchQuery.parse(request.uri.query.params.get("q").getOrElse("")))

  override def plan(connection: Connection, actor: AuthUser, query: UserSearchQuery): IO[List[UserIdentity]] =
    val _ = actor
    if query.value.length < UserApiSupport.minSuggestionQueryLength then IO.pure(Nil)
    else UserTable.listSuggestions(connection, query)
