package domains.judger.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.judge.utils.JudgeConfig

import domains.judger.objects.response.RegisteredJudgerListItem
import domains.judger.table.judger.JudgerTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 站点管理员查看已注册 judger 的 API；会按心跳超时清理过期记录后返回列表。 */
final case class ListRegisteredJudgers(judgeConfig: JudgeConfig) extends SiteManagerApi[Unit, List[RegisteredJudgerListItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/judgers")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[RegisteredJudgerListItem]] = summon[Encoder[List[RegisteredJudgerListItem]]]

  /** 列表接口不需要输入；请求和路径参数仅为框架签名。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = request
    val _ = pathParams
    IO.unit

  /** 返回当前未过期 judger 列表；站点管理员权限由 SiteManagerApi 框架保证。 */
  override def plan(connection: Connection, actor: SiteManagerUser, input: Unit): IO[List[RegisteredJudgerListItem]] =
    val _ = actor
    val _ = input
    JudgerTable.listJudgers(connection, judgeConfig.heartbeatTimeoutMs)
