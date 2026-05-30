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

object CreateConversation extends AuthenticatedApi[CreateConversationRequest, MessageConversationSummary]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/conversations")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageConversationSummary] = summon[Encoder[MessageConversationSummary]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateConversationRequest] =
    val _ = pathParams
    request.as[CreateConversationRequest]

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
