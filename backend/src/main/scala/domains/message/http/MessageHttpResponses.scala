package domains.message.http



import cats.effect.IO
import domains.message.application.MessageCommandResults.{AddBlockResult, CreateConversationResult, GetConversationHistoryResult, MarkConversationReadResult, RemoveBlockResult, SendMessageResult}
import domains.message.http.MessageHttpPlans.{MarkConversationReadOutput, SendMessageOutput}
import domains.shared.http.ApiMessages
import domains.shared.http.utils.HttpResponseSupport.{errorResponse, successResponse}
import io.circe.syntax.*
import org.http4s.{Response, Status}
import org.http4s.circe.CirceEntityEncoder.*

object MessageHttpResponses:

  private def hiddenConversationResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, ApiMessages.directMessageConversationNotFound)

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.utils.HttpResponseSupport.validationErrorResponse(message)

  def inboxResponse(response: domains.message.http.response.MessageInboxResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def historyResponse(result: GetConversationHistoryResult): IO[Response[IO]] =
    result match
      case GetConversationHistoryResult.ConversationNotFound =>
        hiddenConversationResponse
      case GetConversationHistoryResult.Found(history) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(history.asJson))

  def createConversationResponse(result: CreateConversationResult): IO[Response[IO]] =
    result match
      case CreateConversationResult.TargetUserNotFound =>
        errorResponse(Status.NotFound, ApiMessages.userNotFound)
      case CreateConversationResult.CannotMessageSelf =>
        errorResponse(Status.BadRequest, ApiMessages.directMessageSelfForbidden)
      case CreateConversationResult.Ready(conversation) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(conversation.asJson))

  def sendMessageResponse(output: SendMessageOutput): IO[Response[IO]] =
    output.result match
      case SendMessageResult.ConversationNotFound =>
        hiddenConversationResponse
      case SendMessageResult.BlockedByRecipient =>
        errorResponse(Status.Forbidden, ApiMessages.directMessageBlockedByRecipient)
      case SendMessageResult.Sent(message, _) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(message.asJson))

  def markConversationReadResponse(output: MarkConversationReadOutput): IO[Response[IO]] =
    output.result match
      case MarkConversationReadResult.ConversationNotFound =>
        hiddenConversationResponse
      case MarkConversationReadResult.Marked(summary, _, _) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(summary.asJson))

  def markAllMessagesReadResponse(output: domains.message.application.MessageCommandResults.MarkAllMessagesReadResult): IO[Response[IO]] =
    val _ = output
    successResponse(Status.Ok, ApiMessages.directMessagesMarkedRead)

  def listBlocksResponse(entries: List[domains.message.http.response.MessageBlockEntry]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(entries.asJson))

  def addBlockResponse(result: AddBlockResult): IO[Response[IO]] =
    result match
      case AddBlockResult.TargetUserNotFound =>
        errorResponse(Status.NotFound, ApiMessages.userNotFound)
      case AddBlockResult.CannotBlockSelf =>
        errorResponse(Status.BadRequest, ApiMessages.directMessageBlockSelfForbidden)
      case AddBlockResult.Added(entry) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(entry.asJson))

  def removeBlockResponse(result: RemoveBlockResult): IO[Response[IO]] =
    result match
      case RemoveBlockResult.Removed =>
        successResponse(Status.Ok, ApiMessages.directMessageBlockRemoved)
