package domains.judger.api

import cats.effect.IO
import domains.auth.api.PublicApi
import domains.judge.utils.JudgeConfig
import domains.judge.utils.JudgeTokenAuth
import domains.judger.table.judger.JudgerTable
import io.circe.Encoder
import judgeprotocol.objects.request.RegisterJudgerRequest
import judgeprotocol.objects.response.RegisterJudgerResponse
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** judger 注册 API；使用 worker token 认证，分配唯一 judger id 并返回心跳参数。API 对齐例外：这是 worker-only 端点，不提供站点前端 wrapper。 */
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
      validRequest <- HttpApiError.fromEitherBadRequest(validateRegisterRequest(request))
      response <- JudgerTable.register(connection, validRequest, judgeConfig.heartbeatIntervalMs, judgeConfig.heartbeatTimeoutMs)
    yield response

  /** 校验并规范化注册请求；不访问数据库。 */
  private def validateRegisterRequest(request: RegisterJudgerRequest): Either[String, RegisterJudgerRequest] =
    val host = request.host.trim
    if host.isEmpty then Left("Judger host is required.")
    else if request.supportedLanguages.isEmpty then Left("Judger supported languages are required.")
    else
      Right(
        request.copy(
          host = host,
          processId = request.processId.map(_.trim).filter(_.nonEmpty)
        )
      )
