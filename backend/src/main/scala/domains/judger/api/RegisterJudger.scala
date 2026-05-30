package domains.judger.api

import cats.effect.IO
import domains.auth.api.PublicApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTokenAuth
import domains.judger.utils.JudgerRegistryValidation
import domains.judger.table.judger.JudgerTable
import io.circe.Encoder
import judgeprotocol.objects.request.RegisterJudgerRequest
import judgeprotocol.objects.response.RegisterJudgerResponse
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class RegisterJudger(judgeConfig: JudgeConfig) extends PublicApi[RegisterJudgerRequest, RegisterJudgerResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/judgers/register")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RegisterJudgerResponse] = summon[Encoder[RegisterJudgerResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[RegisterJudgerRequest] =
    val _ = pathParams
    JudgeTokenAuth.ensureJudgeToken(request, judgeConfig) *> request.as[RegisterJudgerRequest]

  override def plan(connection: Connection, request: RegisterJudgerRequest): IO[RegisterJudgerResponse] =
    for
      validRequest <- HttpApiError.fromEitherBadRequest(JudgerRegistryValidation.validateRegisterRequest(request))
      response <- JudgerTable.register(connection, validRequest, judgeConfig.heartbeatIntervalMs, judgeConfig.heartbeatTimeoutMs)
    yield response
