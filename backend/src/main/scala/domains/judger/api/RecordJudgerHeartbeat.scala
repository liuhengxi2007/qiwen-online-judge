package domains.judger.api

import cats.effect.IO
import domains.auth.api.PublicApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTokenAuth
import domains.judger.table.judger.JudgerTable
import io.circe.Encoder
import judgeprotocol.objects.JudgerId
import judgeprotocol.objects.request.JudgerHeartbeatRequest
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** judger 心跳上报 API；使用 worker token 认证并延长指定 judger 的租约。 */
final case class RecordJudgerHeartbeat(judgeConfig: JudgeConfig) extends PublicApi[JudgerId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/judgers/:judgerId/heartbeat")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 校验 token、解析 judger id，并确认请求体是合法心跳格式。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[JudgerId] =
    for
      _ <- JudgeTokenAuth.ensureJudgeToken(request, judgeConfig)
      judgerId <- HttpApiError.fromEitherBadRequest(pathParams.require("judgerId").flatMap(JudgerId.parse))
      _ <- request.as[JudgerHeartbeatRequest]
    yield judgerId

  /** 更新活动 judger 的 last_heartbeat_at；不存在或过期时返回 not found。 */
  override def plan(connection: Connection, judgerId: JudgerId): IO[SuccessResponse] =
    JudgerTable.heartbeat(connection, judgerId, judgeConfig.heartbeatTimeoutMs).flatMap {
      case true =>
        IO.pure(SuccessResponse(code = Some(ApiMessages.judgerHeartbeatRecorded.code), message = None, params = ApiMessages.judgerHeartbeatRecorded.params))
      case false =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.judgerNotFoundOrExpired))
    }
