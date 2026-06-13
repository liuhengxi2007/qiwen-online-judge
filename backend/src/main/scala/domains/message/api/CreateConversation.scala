package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.objects.request.CreateConversationRequest
import domains.message.objects.response.MessageConversationSummary
import domains.message.table.message.{MessageConversationTable, MessageUserTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 创建或获取两人私信会话的认证 API。 */
object CreateConversation extends AuthenticatedApi[CreateConversationRequest, MessageConversationSummary]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/conversations")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageConversationSummary] = summon[Encoder[MessageConversationSummary]]

  /** 读取目标用户名请求体，路径参数不参与该入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateConversationRequest] =
    val _ = pathParams
    request.as[CreateConversationRequest]

  /** 拒绝给自己建会话，校验目标账号存在后返回已有或新建的会话摘要。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    request: CreateConversationRequest
  ): IO[MessageConversationSummary] =
    for
      _ <- HttpApiError.ensure(actor.username != request.targetUsername, HttpApiError.badRequest(ApiMessages.directMessageSelfForbidden))
      targetExists <- MessageUserTable.userExists(connection, request.targetUsername)
      _ <- HttpApiError.ensure(targetExists, HttpApiError.notFound(ApiMessages.userNotFound))
      conversation <- MessageConversationTable.getOrCreateConversation(connection, actor.username, request.targetUsername)
    yield conversation
