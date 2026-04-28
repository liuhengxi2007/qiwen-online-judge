package domains.message.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.message.application.MessageCommandResults.GetConversationHistoryResult
import domains.message.model.{MessageConversationId, MessageHistoryResponse, MessageId, MessageInboxResponse}
import domains.message.table.MessageTable

object MessageQueryCommands:
  private val defaultHistoryLimit = 50

  def listInbox(
    databaseSession: DatabaseSession,
    actor: AuthUser
  ): IO[MessageInboxResponse] =
    databaseSession.withTransactionConnection(connection =>
      MessageTable.listInbox(connection, actor.username)
    )

  def getConversationHistory(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int]
  ): IO[GetConversationHistoryResult] =
    databaseSession.withTransactionConnection { connection =>
      for
        conversation <- MessageTable.findConversationSummaryForUser(connection, actor.username, conversationId)
        result <- conversation match
          case None => IO.pure(GetConversationHistoryResult.ConversationNotFound)
          case Some(summary) =>
            MessageTable
              .listConversationMessages(connection, conversationId, beforeMessageId, limit.filter(_ > 0).getOrElse(defaultHistoryLimit))
              .map { case (messages, hasMore) =>
                GetConversationHistoryResult.Found(
                  MessageHistoryResponse(
                    conversation = summary,
                    messages = messages,
                    hasMore = hasMore
                  )
                )
              }
      yield result
    }

  def listBlocks(
    databaseSession: DatabaseSession,
    actor: AuthUser
  ): IO[List[domains.message.model.MessageBlockEntry]] =
    databaseSession.withTransactionConnection(connection =>
      MessageTable.listBlocks(connection, actor.username)
    )
