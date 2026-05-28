package domains.message.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.message.http.codec.MessageHttpCodecs.given
import domains.message.objects.response.MessageHistoryResponse
import domains.message.objects.{MessageConversationId, MessageId}
import domains.message.table.message.{DirectMessageTable, MessageConversationTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetConversationHistory extends AuthenticatedApi[GetConversationHistory.Input, MessageHistoryResponse]:

  private val defaultHistoryLimit = 50

  final case class Input(
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int]
  )

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/conversations/:conversationId/messages")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageHistoryResponse] = summon[Encoder[MessageHistoryResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Input] =
    HttpApiError.fromEitherBadRequest {
      pathParams.require("conversationId").flatMap(MessageConversationId.parse).map { conversationId =>
        Input(
          conversationId = conversationId,
          beforeMessageId = request.uri.query.params.get("before").flatMap(rawId => MessageId.parse(rawId).toOption),
          limit = request.uri.query.params.get("limit").flatMap(_.toIntOption)
        )
      }
    }

  override def plan(
    connection: Connection,
    actor: AuthUser,
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
