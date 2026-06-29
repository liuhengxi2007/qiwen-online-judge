package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.objects.response.MessageInboxResponse
import domains.message.table.message.MessageConversationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.PageRequest

import java.sql.Connection

/** 分页读取当前用户私信收件箱的认证 API。 */
object ListInbox extends AuthenticatedApi[PageRequest, MessageInboxResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/inbox")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageInboxResponse] = summon[Encoder[MessageInboxResponse]]

  /** 从查询参数解析分页信息，路径参数不参与收件箱入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  /** 读取当前用户参与的会话摘要和总未读数。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, pageRequest: PageRequest): IO[MessageInboxResponse] =
    MessageConversationTable.listInbox(connection, actor.username, pageRequest.normalized)
