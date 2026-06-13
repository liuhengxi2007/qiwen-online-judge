package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageEventHubContext, MessageStreamEvent}
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.{MessageBlockTable, MessageUserTable}
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 添加私信屏蔽用户的认证 API，成功后刷新当前用户消息收件箱事件。 */
final class AddMessageBlock(messageEventHub: MessageEventHubContext) extends AuthenticatedApi[Username, MessageBlockEntry]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/blocks/:targetUsername")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageBlockEntry] = summon[Encoder[MessageBlockEntry]]

  /** 从路径解析被屏蔽用户名，用户名会走 canonical 归一化。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername").map(Username.canonical))

  /** 拒绝屏蔽自己，校验目标账号存在后 upsert 屏蔽关系并发布收件箱变更。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[MessageBlockEntry] =
    for
      _ <- HttpApiError.ensure(actor.username != targetUsername, HttpApiError.badRequest(ApiMessages.directMessageBlockSelfForbidden))
      targetExists <- MessageUserTable.userExists(connection, targetUsername)
      _ <- HttpApiError.ensure(targetExists, HttpApiError.notFound(ApiMessages.userNotFound))
      entry <- MessageBlockTable.upsertBlock(connection, actor.username, targetUsername)
      _ <- MessageEventHub.publish(messageEventHub, actor.username, MessageStreamEvent.InboxChanged)
    yield entry
