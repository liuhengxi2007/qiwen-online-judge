package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.usergroup.objects.response.UserGroupSummary
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
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

  override def plan(connection: Connection, actor: AuthenticatedUser, pageRequest: PageRequest): IO[PageResponse[UserGroupSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    if !UserGroupAccessRules.canList(actor) then
      IO.pure(PageResponse(items = Nil, page = normalizedPageRequest.page, pageSize = normalizedPageRequest.pageSize, totalItems = 0L))
    else
      UserGroupTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
