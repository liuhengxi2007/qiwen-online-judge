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

final case class RecordJudgerHeartbeat(judgeConfig: JudgeConfig) extends PublicApi[JudgerId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/judgers/:judgerId/heartbeat")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[JudgerId] =
    for
      _ <- JudgeTokenAuth.ensureJudgeToken(request, judgeConfig)
      judgerId <- HttpApiError.fromEitherBadRequest(pathParams.require("judgerId").flatMap(JudgerId.parse))
      _ <- request.as[JudgerHeartbeatRequest]
    yield judgerId

  override def plan(connection: Connection, judgerId: JudgerId): IO[SuccessResponse] =
    JudgerTable.heartbeat(connection, judgerId, judgeConfig.heartbeatTimeoutMs).flatMap {
      case true =>
        IO.pure(SuccessResponse(code = Some(ApiMessages.judgerHeartbeatRecorded.code), message = None, params = ApiMessages.judgerHeartbeatRecorded.params))
      case false =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.judgerNotFoundOrExpired))
    }
