package domains.message.application



import cats.effect.IO
import cats.syntax.all.*
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.message.application.MessageCommandResults.GetConversationHistoryResult
import domains.message.model.{MessageConversationId, MessageId}
import domains.message.http.response.{MessageHistoryResponse, MessageInboxResponse}
import domains.shared.model.PageRequest

object MessageQueryCommands:
  private val defaultHistoryLimit = 50
  private val defaultRepository: MessageRepository = JdbcMessageRepository

  def listInbox(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest,
    repository: MessageRepository = defaultRepository
  ): IO[MessageInboxResponse] =
    databaseSession.withTransactionConnection(connection =>
      repository.listInbox(connection, actor.username, pageRequest.normalized)
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
            (
              repository.listConversationMessages(connection, conversationId, beforeMessageId, limit.filter(_ > 0).getOrElse(defaultHistoryLimit)),
              repository.getConversationMessageFacts(connection, conversationId, actor.username)
            ).mapN { case ((messages, hasMore), facts) =>
                GetConversationHistoryResult.Found(
                  MessageHistoryResponse(
                    conversation = summary,
                    messages = messages,
                    hasMore = hasMore,
                    facts = facts
                  )
                )
              }
      yield result
    }

  def listBlocks(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    repository: MessageRepository = defaultRepository
  ): IO[List[domains.message.http.response.MessageBlockEntry]] =
    databaseSession.withTransactionConnection(connection =>
      repository.listBlocks(connection, actor.username)
    )
