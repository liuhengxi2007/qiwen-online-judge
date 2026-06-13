package judger.http

import cats.effect.IO
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.{JudgerId, ProblemSlug, SubmissionId}
import judgeprotocol.objects.request.{ClaimJudgeTaskRequest, JudgerHeartbeatRequest, RegisterJudgerRequest, ReportHackResultRequest, ReportJudgeResultRequest}
import judgeprotocol.objects.response.{JudgeWorkerTask, RegisterJudgerResponse}
import judger.config.AppConfig

import java.net.{URI, URLEncoder}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration

/** backend 表示当前 judger 租约已不存在时在客户端侧使用的控制异常。 */
final case class LeaseExpiredException(message: String) extends RuntimeException(message)

/** 题目数据下载边界，便于缓存层依赖协议而不是具体 HTTP 客户端。 */
trait ProblemDataDownloader:
  /** 下载某题某个相对路径的数据字节；调用方负责校验 sha256。 */
  def downloadProblemData(problemSlug: ProblemSlug, path: String): IO[Array[Byte]]

/** judger 访问 backend worker API 的 HTTP 客户端，负责鉴权 header、JSON 编解码和状态码解释。 */
final class JudgeHttpClient(httpClient: HttpClient, config: AppConfig) extends ProblemDataDownloader:
  /** 注册当前 judger 能力并获取 backend 分配的租约。 */
  def registerJudger: IO[RegisterJudgerResponse] =
    requestExpectJson[RegisterJudgerResponse](
      path = "/api/worker/judge/judgers/register",
      method = "POST",
      body = Some(
        RegisterJudgerRequest(
          preferredPrefix = config.preferredJudgerPrefix,
          host = config.host,
          processId = config.processId,
          supportedLanguages = config.supportedLanguages
        ).asJson.noSpaces
      )
    )

  /** 向 backend 上报一次心跳；404 被解释为租约过期并交给调用方重注册。 */
  def heartbeat(judgerId: JudgerId): IO[Unit] =
    requestRaw(
      path = s"/api/worker/judge/judgers/${URLEncoder.encode(judgerId.value, StandardCharsets.UTF_8)}/heartbeat",
      method = "POST",
      body = Some(JudgerHeartbeatRequest().asJson.noSpaces)
    ).flatMap(handleHeartbeatResponse(judgerId, _))

  /** 领取一个普通判题或 hack 任务；204 表示当前没有任务。 */
  def claimTask(judgerId: JudgerId): IO[Option[JudgeWorkerTask]] =
    requestRaw(
      path = "/api/worker/judge/claim",
      method = "POST",
      body = Some(ClaimJudgeTaskRequest(judgerId).asJson.noSpaces)
    ).flatMap { response =>
      if response.statusCode == 204 then IO.pure(None)
      else if !response.isSuccess then
        requestFailed("Claim failed", response)
      else
        IO.fromEither(decode[JudgeWorkerTask](response.body).left.map(error => RuntimeException(error.getMessage))).map(Some(_))
    }

  /** 回报普通提交判题结果；backend 会校验状态转换和提交存在性。 */
  def reportResult(submissionId: SubmissionId, result: ReportJudgeResultRequest): IO[Unit] =
    requestExpectSuccess(
      path = s"/api/worker/judge/submissions/${submissionId.value}/complete",
      method = "POST",
      body = Some(result.asJson.noSpaces)
    ).void

  /** 回报 hack 尝试结果；status 字符串由 backend HackStatus 校验。 */
  def reportHackResult(hackId: Long, result: ReportHackResultRequest): IO[Unit] =
    requestExpectSuccess(
      path = s"/api/worker/judge/hacks/$hackId/complete",
      method = "POST",
      body = Some(result.asJson.noSpaces)
    ).void

  /** 从 backend 下载题目数据文件；只做传输，不在此处校验 hash。 */
  def downloadProblemData(problemSlug: ProblemSlug, path: String): IO[Array[Byte]] =
    requestBytes(
      path =
        s"/api/worker/judge/problem-data?problemSlug=${encode(problemSlug.value)}&path=${encode(path)}",
      method = "GET"
    )

  private def requestExpectSuccess(path: String, method: String, body: Option[String]): IO[String] =
    requestRaw(path, method, body).flatMap { response =>
      if response.isSuccess then IO.pure(response.body)
      else requestFailed("Request failed", response)
    }

  private def requestExpectJson[A: io.circe.Decoder](path: String, method: String, body: Option[String]): IO[A] =
    requestExpectSuccess(path, method, body).flatMap { responseBody =>
      IO.fromEither(decode[A](responseBody).left.map(error => RuntimeException(error.getMessage)))
    }

  private def requestRaw(path: String, method: String, body: Option[String]): IO[HttpResponse[String]] =
    IO.blocking {
      val builder = HttpRequest
        .newBuilder(URI.create(s"${config.backendBaseUrl}$path"))
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .header("x-judge-token", config.judgeToken)

      val request =
        body match
          case Some(value) => builder.method(method, HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8)).build()
          case None => builder.method(method, HttpRequest.BodyPublishers.noBody()).build()

      httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }

  private def requestBytes(path: String, method: String): IO[Array[Byte]] =
    IO.blocking {
      val request = HttpRequest
        .newBuilder(URI.create(s"${config.backendBaseUrl}$path"))
        .timeout(Duration.ofSeconds(30))
        .header("x-judge-token", config.judgeToken)
        .method(method, HttpRequest.BodyPublishers.noBody())
        .build()
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
      if response.isSuccess then response.body
      // FIXME-CN: 题目数据下载失败只抛 RuntimeException，未区分 403/404/5xx，排查数据权限或版本漂移时信息不足。
      else throw RuntimeException(s"Request failed with HTTP ${response.statusCode}: ${new String(response.body, StandardCharsets.UTF_8)}")
    }

  private def encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def handleHeartbeatResponse(judgerId: JudgerId, response: HttpResponse[String]): IO[Unit] =
    if response.isSuccess then IO.unit
    // 注意：worker 心跳接口的 404 代表 judger 租约已过期，不按普通隐藏资源查询处理。
    else if response.statusCode == 404 then IO.raiseError(LeaseExpiredException(s"Judger lease expired for ${judgerId.value}."))
    else requestFailed("Request failed", response)

  private def requestFailed[A](prefix: String, response: HttpResponse[String]): IO[A] =
    IO.raiseError(RuntimeException(s"$prefix with HTTP ${response.statusCode}: ${response.body}"))

/** JudgeHttpClient 的构造入口，集中配置底层 JDK HttpClient。 */
object JudgeHttpClient:
  /** 创建带连接超时的 JDK HTTP 客户端实例。 */
  def create(config: AppConfig): JudgeHttpClient =
    new JudgeHttpClient(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
      config
    )

extension (response: HttpResponse[?])
  private def isSuccess: Boolean =
    response.statusCode >= 200 && response.statusCode < 300
