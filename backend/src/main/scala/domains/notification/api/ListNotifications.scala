package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.notification.objects.response.NotificationListResponse
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
import shared.objects.PageRequest

import java.sql.Connection

object ListNotifications extends AuthenticatedApi[PageRequest, NotificationListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/notifications")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[NotificationListResponse] = summon[Encoder[NotificationListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[NotificationListResponse] =
    NotificationTable.listForRecipient(connection, actor.username, pageRequest.normalized)
