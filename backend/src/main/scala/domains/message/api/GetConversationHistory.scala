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
  private val maxHistoryLimit = 100

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

  /** 从路径解析会话 id，并从查询参数读取 before 和 limit；无效查询参数返回 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Input] =
    val queryParams = request.uri.query.params
    HttpApiError.fromEitherBadRequest {
      for
        conversationId <- pathParams.require("conversationId").flatMap(MessageConversationId.parse)
        beforeMessageId <- queryParams.get("before").map(rawId => MessageId.parse(rawId).map(Some(_))).getOrElse(Right(None))
        limit <- parseLimit(queryParams.get("limit"))
      yield Input(conversationId = conversationId, beforeMessageId = beforeMessageId, limit = limit)
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
        input.limit.getOrElse(defaultHistoryLimit)
      )
      facts <- DirectMessageTable.getConversationMessageFacts(connection, input.conversationId, actor.username)
      (messages, hasMore) = messagesAndMore
    yield MessageHistoryResponse(
      conversation = conversation,
      messages = messages,
      hasMore = hasMore,
      facts = facts
    )

  private def parseLimit(rawLimit: Option[String]): Either[String, Option[Int]] =
    rawLimit match
      case None => Right(None)
      case Some(raw) =>
        raw.trim.toIntOption match
          case Some(value) if value >= 1 && value <= maxHistoryLimit => Right(Some(value))
          case Some(_) => Left(s"limit must be between 1 and $maxHistoryLimit.")
          case None => Left("limit must be a positive integer.")
