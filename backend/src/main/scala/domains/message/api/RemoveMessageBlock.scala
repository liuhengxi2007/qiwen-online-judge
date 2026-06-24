package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageEventHubContext, MessageStreamEvent}
import domains.message.table.message.MessageBlockTable
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 移除当前用户私信屏蔽关系的认证 API，成功后刷新当前用户收件箱事件。 */
final class RemoveMessageBlock(messageEventHub: MessageEventHubContext) extends AuthenticatedApi[Username, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/blocks/:targetUsername/unlink")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析目标用户名，用户名会走 canonical 归一化。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername").map(Username.canonical))

  /** 删除屏蔽关系；关系原本不存在也返回成功并发布当前用户收件箱变更。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[SuccessResponse] =
    for
      _ <- MessageBlockTable.removeBlock(connection, actor.username, targetUsername)
      _ <- MessageEventHub.publish(messageEventHub, actor.username, MessageStreamEvent.InboxChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.directMessageBlockRemoved.code),
      message = None,
      params = ApiMessages.directMessageBlockRemoved.params
    )
