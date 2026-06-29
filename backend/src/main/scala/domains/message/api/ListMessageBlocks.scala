package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.MessageBlockTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 列出当前用户私信屏蔽名单的认证 API。 */
object ListMessageBlocks extends AuthenticatedApi[Unit, List[MessageBlockEntry]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/blocks")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[MessageBlockEntry]] = Encoder.encodeList[MessageBlockEntry]

  /** 屏蔽列表查询不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 按当前用户读取屏蔽名单，返回被屏蔽用户身份和屏蔽时间。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: Unit): IO[List[MessageBlockEntry]] =
    val _ = input
    MessageBlockTable.listBlocks(connection, actor.username)
