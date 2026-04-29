package domains.message.application

import cats.effect.IO
import domains.auth.model.{AuthUser, DisplayName, EmailAddress, PasswordHash, Username}
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.model.{CreateConversationRequest, DirectMessage, MarkConversationReadRequest, MessageBlockEntry, MessageContent, MessageConversationId, MessageConversationSummary, MessageId, MessageInboxResponse, SendDirectMessageRequest}
import domains.problem.model.ProblemTitleDisplayMode
import domains.user.model.{UserDisplayMode, UserIdentity, UserLocale}
import munit.FunSuite

import java.sql.Connection
import java.time.Instant
import java.util.UUID
import cats.effect.unsafe.implicits.global

class MessageMutationCommandsSuite extends FunSuite:

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

    val result = MessageMutationCommands.createConversation(connection, actor, request, FakeMessageRepository()).unsafeRunSync()
    assertEquals(result, CreateConversationResult.CannotMessageSelf)
  }

  test("createConversation returns TargetUserNotFound when target user does not exist") {
    val repository = FakeMessageRepository(userExistsResult = false)
    val request = CreateConversationRequest(targetUsername)

    val result = MessageMutationCommands.createConversation(connection, actor, request, repository).unsafeRunSync()
    assertEquals(result, CreateConversationResult.TargetUserNotFound)
  }

  test("createConversation returns Ready for a valid target user") {
    val repository = FakeMessageRepository(conversationToReturn = summary)
    val request = CreateConversationRequest(targetUsername)

    val result = MessageMutationCommands.createConversation(connection, actor, request, repository).unsafeRunSync()
    assertEquals(result, CreateConversationResult.Ready(summary))
  }

  test("sendMessage returns ConversationNotFound when the conversation is not visible") {
    val repository = FakeMessageRepository(otherParticipant = None)
    val request = SendDirectMessageRequest(MessageContent("hello"))

    val result = MessageMutationCommands.sendMessage(connection, actor, conversationId, request, repository).unsafeRunSync()
    assertEquals(result, SendMessageResult.ConversationNotFound)
  }

  test("sendMessage returns BlockedByRecipient when the recipient has blocked the actor") {
    val repository = FakeMessageRepository(otherParticipant = Some(targetUsername), blockedResult = true)
    val request = SendDirectMessageRequest(MessageContent("hello"))

    val result = MessageMutationCommands.sendMessage(connection, actor, conversationId, request, repository).unsafeRunSync()
    assertEquals(result, SendMessageResult.BlockedByRecipient)
  }

  test("sendMessage returns Sent when message insertion succeeds") {
    val repository = FakeMessageRepository(
      otherParticipant = Some(targetUsername),
      insertedMessage = directMessage
    )
    val request = SendDirectMessageRequest(MessageContent("hello"))

    val result = MessageMutationCommands.sendMessage(connection, actor, conversationId, request, repository).unsafeRunSync()
    assertEquals(result, SendMessageResult.Sent(directMessage, targetUsername))
  }

  test("markConversationRead returns ConversationNotFound when the conversation summary is missing") {
    val repository = FakeMessageRepository(summaryForUser = None)

    val result =
      MessageMutationCommands.markConversationRead(connection, actor, conversationId, MarkConversationReadRequest(), repository).unsafeRunSync()
    assertEquals(result, MarkConversationReadResult.ConversationNotFound)
  }

  test("markConversationRead returns Marked when the conversation is visible") {
    val repository = FakeMessageRepository(
      summaryForUser = Some(summary),
      otherParticipant = Some(targetUsername),
      readUpToMessageId = Some(messageId)
    )

    val result =
      MessageMutationCommands.markConversationRead(connection, actor, conversationId, MarkConversationReadRequest(), repository).unsafeRunSync()
    assertEquals(result, MarkConversationReadResult.Marked(summary, targetUsername, Some(messageId)))
  }

  test("addBlock returns CannotBlockSelf when actor targets self") {
    val result = MessageMutationCommands.addBlock(connection, actor, actor.username, FakeMessageRepository()).unsafeRunSync()
    assertEquals(result, AddBlockResult.CannotBlockSelf)
  }

  test("addBlock returns TargetUserNotFound when target user does not exist") {
    val repository = FakeMessageRepository(userExistsResult = false)

    val result = MessageMutationCommands.addBlock(connection, actor, targetUsername, repository).unsafeRunSync()
    assertEquals(result, AddBlockResult.TargetUserNotFound)
  }

  test("addBlock returns Added when the block entry is created") {
    val repository = FakeMessageRepository(blockEntryToReturn = blockEntry)

    val result = MessageMutationCommands.addBlock(connection, actor, targetUsername, repository).unsafeRunSync()
    assertEquals(result, AddBlockResult.Added(blockEntry))
  }

  test("removeBlock returns Removed") {
    val result = MessageMutationCommands.removeBlock(connection, actor, targetUsername, FakeMessageRepository()).unsafeRunSync()
    assertEquals(result, RemoveBlockResult.Removed)
  }

  private def sampleActor(username: String): AuthUser =
    AuthUser(
      username = Username.canonical(username),
      displayName = DisplayName(username.capitalize),
      email = EmailAddress(s"$username@example.com"),
      displayMode = UserDisplayMode.DisplayName,
      locale = UserLocale.En,
      problemTitleDisplayMode = ProblemTitleDisplayMode.Title,
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
    blockEntryToReturn: MessageBlockEntry = blockEntry
  ) extends MessageRepository:

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

    override def listInbox(connection: Connection, actorUsername: Username): IO[MessageInboxResponse] =
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

    override def markConversationRead(
      connection: Connection,
      conversationId: MessageConversationId,
      recipientUsername: Username
    ): IO[Option[MessageId]] =
      IO.pure(readUpToMessageId)

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
