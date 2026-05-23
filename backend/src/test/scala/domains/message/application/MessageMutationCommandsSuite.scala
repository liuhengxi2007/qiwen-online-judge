package domains.message.application

import cats.effect.IO
import domains.auth.model.{AuthUser, EmailAddress, PasswordHash}
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.application.input.{CreateConversationRequest, MarkConversationReadMode, MarkConversationReadRequest, SendDirectMessageRequest}
import domains.message.application.output.{ConversationMessageFacts, DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageInboxResponse}
import domains.message.model.{ConversationReadReceipt, MessageContent, MessageConversationId, MessageId}
import domains.problem.model.ProblemTitleDisplayMode
import domains.user.model.{DisplayName, UserDisplayMode, UserIdentity, UserLocale, Username}
import munit.CatsEffectSuite
import shared.model.PageRequest

import java.sql.Connection
import java.time.Instant
import java.util.UUID

class MessageMutationCommandsSuite extends CatsEffectSuite:

  private val connection = null.asInstanceOf[Connection]
  private val actor = sampleActor("alice")
  private val targetUsername = Username.canonical("bob")
  private val conversationId = MessageConversationId(UUID.fromString("11111111-1111-4111-8111-111111111111"))
  private val messageId = MessageId(UUID.fromString("22222222-2222-4222-8222-222222222222"))
  private val summary = sampleSummary(conversationId, targetUsername)
  private val directMessage = sampleDirectMessage(conversationId, actor.username, targetUsername)
  private val blockEntry = MessageBlockEntry(
    user = UserIdentity(targetUsername, DisplayName("Bob")),
    createdAt = Instant.parse("2026-04-29T12:00:00Z")
  )

  test("createConversation returns CannotMessageSelf when actor targets self") {
    val request = CreateConversationRequest(actor.username)

    MessageMutationCommands
      .createConversation(connection, actor, request, FakeMessageRepository())
      .map(result => assertEquals(result, CreateConversationResult.CannotMessageSelf))
  }

  test("createConversation returns TargetUserNotFound when target user does not exist") {
    val repository = FakeMessageRepository(userExistsResult = false)
    val request = CreateConversationRequest(targetUsername)

    MessageMutationCommands
      .createConversation(connection, actor, request, repository)
      .map(result => assertEquals(result, CreateConversationResult.TargetUserNotFound))
  }

  test("createConversation returns Ready for a valid target user") {
    val repository = FakeMessageRepository(conversationToReturn = summary)
    val request = CreateConversationRequest(targetUsername)

    MessageMutationCommands
      .createConversation(connection, actor, request, repository)
      .map(result => assertEquals(result, CreateConversationResult.Ready(summary)))
  }

  test("sendMessage returns ConversationNotFound when the conversation is not visible") {
    val repository = FakeMessageRepository(otherParticipant = None)
    val request = SendDirectMessageRequest(MessageContent("hello"))

    MessageMutationCommands
      .sendMessage(connection, actor, conversationId, request, repository)
      .map(result => assertEquals(result, SendMessageResult.ConversationNotFound))
  }

  test("sendMessage returns BlockedByRecipient when the recipient has blocked the actor") {
    val repository = FakeMessageRepository(otherParticipant = Some(targetUsername), blockedResult = true)
    val request = SendDirectMessageRequest(MessageContent("hello"))

    MessageMutationCommands
      .sendMessage(connection, actor, conversationId, request, repository)
      .map(result => assertEquals(result, SendMessageResult.BlockedByRecipient))
  }

  test("sendMessage returns Sent when message insertion succeeds") {
    val repository = FakeMessageRepository(
      otherParticipant = Some(targetUsername),
      insertedMessage = directMessage
    )
    val request = SendDirectMessageRequest(MessageContent("hello"))

    MessageMutationCommands
      .sendMessage(connection, actor, conversationId, request, repository)
      .map(result => assertEquals(result, SendMessageResult.Sent(directMessage, targetUsername)))
  }

  test("markConversationRead returns ConversationNotFound when the conversation summary is missing") {
    val repository = FakeMessageRepository(summaryForUser = None)

    MessageMutationCommands
      .markConversationRead(connection, actor, conversationId, MarkConversationReadRequest(MarkConversationReadMode.Conversation, None), repository)
      .map(result => assertEquals(result, MarkConversationReadResult.ConversationNotFound))
  }

  test("markConversationRead returns Marked when the conversation is visible") {
    val repository = FakeMessageRepository(
      summaryForUser = Some(summary),
      otherParticipant = Some(targetUsername),
      readUpToMessageId = Some(messageId)
    )

    MessageMutationCommands
      .markConversationRead(connection, actor, conversationId, MarkConversationReadRequest(MarkConversationReadMode.Conversation, None), repository)
      .map(result => assertEquals(result, MarkConversationReadResult.Marked(summary, targetUsername, Some(messageId))))
  }

  test("markConversationRead returns the selected message id when message mode updates one unread message") {
    val repository = FakeMessageRepository(
      summaryForUser = Some(summary),
      otherParticipant = Some(targetUsername),
      markMessageReadResult = true
    )

    MessageMutationCommands
      .markConversationRead(connection, actor, conversationId, MarkConversationReadRequest(MarkConversationReadMode.Message, Some(messageId)), repository)
      .map(result => assertEquals(result, MarkConversationReadResult.Marked(summary, targetUsername, Some(messageId))))
  }

  test("markAllMessagesRead delegates to the repository") {
    val receipt = ConversationReadReceipt(conversationId, targetUsername, messageId)
    val repository = FakeMessageRepository(unreadConversationReadReceipts = List(receipt))

    MessageMutationCommands.markAllMessagesRead(connection, actor, repository).map { result =>
      assertEquals(repository.markAllMessagesReadCalled, true)
      assertEquals(result.receipts, List(receipt))
    }
  }

  test("addBlock returns CannotBlockSelf when actor targets self") {
    MessageMutationCommands
      .addBlock(connection, actor, actor.username, FakeMessageRepository())
      .map(result => assertEquals(result, AddBlockResult.CannotBlockSelf))
  }

  test("addBlock returns TargetUserNotFound when target user does not exist") {
    val repository = FakeMessageRepository(userExistsResult = false)

    MessageMutationCommands
      .addBlock(connection, actor, targetUsername, repository)
      .map(result => assertEquals(result, AddBlockResult.TargetUserNotFound))
  }

  test("addBlock returns Added when the block entry is created") {
    val repository = FakeMessageRepository(blockEntryToReturn = blockEntry)

    MessageMutationCommands
      .addBlock(connection, actor, targetUsername, repository)
      .map(result => assertEquals(result, AddBlockResult.Added(blockEntry)))
  }

  test("removeBlock returns Removed") {
    MessageMutationCommands
      .removeBlock(connection, actor, targetUsername, FakeMessageRepository())
      .map(result => assertEquals(result, RemoveBlockResult.Removed))
  }

  private def sampleActor(username: String): AuthUser =
    AuthUser(
      username = Username.canonical(username),
      displayName = DisplayName(username.capitalize),
      email = EmailAddress(s"$username@example.com"),
      displayMode = UserDisplayMode.DisplayName,
      locale = UserLocale.En,
      problemTitleDisplayMode = ProblemTitleDisplayMode.Title,
      autoMarkMessageRead = false,
      passwordHash = PasswordHash("hashed"),
      siteManager = false,
      problemManager = false
    )

  private def sampleSummary(conversationId: MessageConversationId, otherUsername: Username): MessageConversationSummary =
    MessageConversationSummary(
      id = conversationId,
      otherUser = UserIdentity(otherUsername, DisplayName("Bob")),
      lastMessagePreview = Some("hello"),
      lastMessageSenderUsername = Some(otherUsername),
      lastMessageAt = Instant.parse("2026-04-29T12:00:00Z"),
      unreadCount = 1
    )

  private def sampleDirectMessage(
    conversationId: MessageConversationId,
    senderUsername: Username,
    recipientUsername: Username
  ): DirectMessage =
    DirectMessage(
      id = messageId,
      conversationId = conversationId,
      sender = UserIdentity(senderUsername, DisplayName("Alice")),
      recipientUsername = recipientUsername,
      content = MessageContent("hello"),
      createdAt = Instant.parse("2026-04-29T12:00:00Z"),
      readAt = None
    )

  private final case class FakeMessageRepository(
    userExistsResult: Boolean = true,
    conversationToReturn: MessageConversationSummary = summary,
    summaryForUser: Option[MessageConversationSummary] = Some(summary),
    otherParticipant: Option[Username] = Some(targetUsername),
    insertedMessage: DirectMessage = directMessage,
    blockedResult: Boolean = false,
    readUpToMessageId: Option[MessageId] = Some(messageId),
    markMessageReadResult: Boolean = false,
    unreadConversationReadReceipts: List[ConversationReadReceipt] = Nil,
    blockEntryToReturn: MessageBlockEntry = blockEntry
  ) extends MessageRepository:
    var markAllMessagesReadCalled: Boolean = false

    override def userExists(connection: Connection, username: Username): IO[Boolean] =
      IO.pure(userExistsResult)

    override def getOrCreateConversation(
      connection: Connection,
      actorUsername: Username,
      targetUsername: Username
    ): IO[MessageConversationSummary] =
      IO.pure(conversationToReturn)

    override def findConversationSummaryForUser(
      connection: Connection,
      actorUsername: Username,
      conversationId: MessageConversationId
    ): IO[Option[MessageConversationSummary]] =
      IO.pure(summaryForUser)

    override def listInbox(connection: Connection, actorUsername: Username, pageRequest: PageRequest): IO[MessageInboxResponse] =
      IO.raiseError(new UnsupportedOperationException("not used in this suite"))

    override def findOtherParticipant(
      connection: Connection,
      actorUsername: Username,
      conversationId: MessageConversationId
    ): IO[Option[Username]] =
      IO.pure(otherParticipant)

    override def listConversationMessages(
      connection: Connection,
      conversationId: MessageConversationId,
      beforeMessageId: Option[MessageId],
      limit: Int
    ): IO[(List[DirectMessage], Boolean)] =
      IO.raiseError(new UnsupportedOperationException("not used in this suite"))

    override def insertMessage(
      connection: Connection,
      conversationId: MessageConversationId,
      senderUsername: Username,
      recipientUsername: Username,
      content: MessageContent
    ): IO[DirectMessage] =
      IO.pure(insertedMessage)

    override def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean] =
      IO.pure(blockedResult)

    override def getConversationMessageFacts(
      connection: Connection,
      conversationId: MessageConversationId,
      actorUsername: Username
    ): IO[ConversationMessageFacts] =
      IO.raiseError(new UnsupportedOperationException("not used in this suite"))

    override def markConversationRead(
      connection: Connection,
      conversationId: MessageConversationId,
      recipientUsername: Username
    ): IO[Option[MessageId]] =
      IO.pure(readUpToMessageId)

    override def markMessageRead(
      connection: Connection,
      conversationId: MessageConversationId,
      recipientUsername: Username,
      messageId: MessageId
    ): IO[Boolean] =
      IO.pure(markMessageReadResult)

    override def markAllMessagesRead(connection: Connection, recipientUsername: Username): IO[Unit] =
      IO {
        markAllMessagesReadCalled = true
      }

    override def listUnreadConversationReadReceipts(
      connection: Connection,
      recipientUsername: Username
    ): IO[List[ConversationReadReceipt]] =
      IO.pure(unreadConversationReadReceipts)

    override def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]] =
      IO.raiseError(new UnsupportedOperationException("not used in this suite"))

    override def upsertBlock(
      connection: Connection,
      ownerUsername: Username,
      blockedUsername: Username
    ): IO[MessageBlockEntry] =
      IO.pure(blockEntryToReturn)

    override def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit] =
      IO.unit
