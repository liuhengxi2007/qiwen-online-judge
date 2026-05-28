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

final case class ListRegisteredJudgers(judgeConfig: JudgeConfig) extends SiteManagerApi[Unit, List[RegisteredJudgerListItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/judgers")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[RegisteredJudgerListItem]] = summon[Encoder[List[RegisteredJudgerListItem]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = request
    val _ = pathParams
    IO.unit

  override def plan(connection: Connection, actor: SiteManagerUser, input: Unit): IO[List[RegisteredJudgerListItem]] =
    val _ = actor
    val _ = input
    JudgerTable.listJudgers(connection, judgeConfig.heartbeatTimeoutMs)
