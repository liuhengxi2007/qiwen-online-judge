package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.objects.response.MessageHistoryResponse
import domains.message.objects.{MessageConversationId, MessageId}
import domains.message.table.message.{DirectMessageTable, MessageConversationTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 获取当前用户某个私信会话历史消息的认证 API。 */
object GetConversationHistory extends AuthenticatedApi[GetConversationHistory.Input, MessageHistoryResponse]:

  private val defaultHistoryLimit = 50

  /** 会话历史查询输入，支持按 beforeMessageId 向前翻页和自定义正数 limit。 */
  final case class Input(
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int]
  )

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/conversations/:conversationId/messages")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageHistoryResponse] = summon[Encoder[MessageHistoryResponse]]

  /** 从路径解析会话 id，并从查询参数读取 before 和 limit；无效 before 会被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Input] =
    HttpApiError.fromEitherBadRequest {
      pathParams.require("conversationId").flatMap(MessageConversationId.parse).map { conversationId =>
        Input(
          conversationId = conversationId,
          /** FIXME-CN: before 参数解析失败会被 flatMap(...toOption) 静默忽略，客户端传入非法游标时不会得到 400，可能导致分页从最新消息重新开始。 */
          beforeMessageId = request.uri.query.params.get("before").flatMap(rawId => MessageId.parse(rawId).toOption),
          /** FIXME-CN: limit 参数解析失败会被静默忽略，且后续只校验正数不设上限，恶意大 limit 可能造成过大的历史消息查询。 */
          limit = request.uri.query.params.get("limit").flatMap(_.toIntOption)
        )
      }
    }

  /** 校验当前用户属于会话后读取历史消息、更多标记和会话消息事实。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: Input
  ): IO[MessageHistoryResponse] =
    for
      maybeConversation <- MessageConversationTable.findConversationSummaryForUser(connection, actor.username, input.conversationId)
      conversation <- maybeConversation match
        case Some(conversation) => IO.pure(conversation)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.directMessageConversationNotFound))
      messagesAndMore <- DirectMessageTable.listConversationMessages(
        connection,
        input.conversationId,
        input.beforeMessageId,
        input.limit.filter(_ > 0).getOrElse(defaultHistoryLimit)
      )
      facts <- DirectMessageTable.getConversationMessageFacts(connection, input.conversationId, actor.username)
      (messages, hasMore) = messagesAndMore
    yield MessageHistoryResponse(
      conversation = conversation,
      messages = messages,
      hasMore = hasMore,
      facts = facts
    )
