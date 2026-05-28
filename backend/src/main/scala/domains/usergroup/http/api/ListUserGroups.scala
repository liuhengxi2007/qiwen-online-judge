package domains.usergroup.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import domains.usergroup.objects.response.UserGroupSummary
import domains.usergroup.rules.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.utils.PageRequestQuerySupport
import shared.http.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

object ListUserGroups extends AuthenticatedApi[PageRequest, PageResponse[UserGroupSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/user-groups")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[UserGroupSummary]] = summon[Encoder[PageResponse[UserGroupSummary]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  override def plan(connection: Connection, actor: AuthUser, pageRequest: PageRequest): IO[PageResponse[UserGroupSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    if !UserGroupAccessRules.canList(actor) then
      IO.pure(PageResponse(items = Nil, page = normalizedPageRequest.page, pageSize = normalizedPageRequest.pageSize, totalItems = 0L))
    else
      UserGroupTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
