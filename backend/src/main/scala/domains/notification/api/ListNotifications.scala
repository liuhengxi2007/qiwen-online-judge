package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.notification.objects.response.NotificationListResponse
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
import shared.objects.PageRequest

import java.sql.Connection

/** 分页读取当前用户通知列表的认证 API，同时返回未读总数。 */
object ListNotifications extends AuthenticatedApi[PageRequest, NotificationListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/notifications")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[NotificationListResponse] = summon[Encoder[NotificationListResponse]]

  /** 从查询参数解析分页信息，路径参数不参与通知列表入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  /** 读取当前用户通知列表，严格按收件人用户名过滤。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    pageRequest: PageRequest
  ): IO[NotificationListResponse] =
    NotificationTable.listForRecipient(connection, actor.username, pageRequest.normalized)
