package domains.message.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.message.application.MessageCommandResults.GetConversationHistoryResult
import domains.message.model.{MessageConversationId, MessageHistoryResponse, MessageId, MessageInboxResponse}

object MessageQueryCommands:
  private val defaultHistoryLimit = 50
  private val defaultRepository: MessageRepository = JdbcMessageRepository

  def listInbox(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    repository: MessageRepository = defaultRepository
  ): IO[MessageInboxResponse] =
    databaseSession.withTransactionConnection(connection =>
      repository.listInbox(connection, actor.username)
    )

  def getConversationHistory(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    conversationId: MessageConversationId,
    beforeMessageId: Option[MessageId],
    limit: Option[Int],
    repository: MessageRepository = defaultRepository
  ): IO[GetConversationHistoryResult] =
    databaseSession.withTransactionConnection { connection =>
      for
        conversation <- repository.findConversationSummaryForUser(connection, actor.username, conversationId)
        result <- conversation match
          case None => IO.pure(GetConversationHistoryResult.ConversationNotFound)
          case Some(summary) =>
            repository
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
    actor: AuthUser,
    repository: MessageRepository = defaultRepository
  ): IO[List[domains.message.model.MessageBlockEntry]] =
    databaseSession.withTransactionConnection(connection =>
      repository.listBlocks(connection, actor.username)
    )
