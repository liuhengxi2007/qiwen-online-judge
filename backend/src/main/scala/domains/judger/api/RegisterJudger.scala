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

/** judger 注册 API；使用 worker token 认证，分配唯一 judger id 并返回心跳参数。 */
final case class RegisterJudger(judgeConfig: JudgeConfig) extends PublicApi[RegisterJudgerRequest, RegisterJudgerResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/worker/judge/judgers/register")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RegisterJudgerResponse] = summon[Encoder[RegisterJudgerResponse]]

  /** 校验 token 并解析注册请求体；路径参数无业务含义。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[RegisterJudgerRequest] =
    val _ = pathParams
    JudgeTokenAuth.ensureJudgeToken(request, judgeConfig) *> request.as[RegisterJudgerRequest]

  /** 校验注册字段并写入 judger 表；会清理过期记录再分配 id。 */
  override def plan(connection: Connection, request: RegisterJudgerRequest): IO[RegisterJudgerResponse] =
    for
      validRequest <- HttpApiError.fromEitherBadRequest(JudgerRegistryValidation.validateRegisterRequest(request))
      response <- JudgerTable.register(connection, validRequest, judgeConfig.heartbeatIntervalMs, judgeConfig.heartbeatTimeoutMs)
    yield response
