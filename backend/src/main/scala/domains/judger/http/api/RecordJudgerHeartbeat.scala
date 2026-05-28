package domains.judger.http.api

import cats.effect.IO
import domains.auth.http.PublicApi
import domains.judge.utils.JudgeConfig
import domains.judge.http.JudgeApiSupport
import domains.judger.table.judger.JudgerTable
import io.circe.Encoder
import judgeprotocol.objects.{JudgerHeartbeatRequest, JudgerId}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class RecordJudgerHeartbeat(judgeConfig: JudgeConfig) extends PublicApi[JudgerId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/judgers/:judgerId/heartbeat")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[JudgerId] =
    for
      _ <- JudgeApiSupport.ensureJudgeToken(request, judgeConfig)
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
