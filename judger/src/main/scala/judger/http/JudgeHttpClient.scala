package judger.http

import cats.effect.IO
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.{JudgerId, ProblemSlug, SubmissionId}
import judgeprotocol.objects.request.{ClaimJudgeTaskRequest, JudgerHeartbeatRequest, RegisterJudgerRequest, ReportJudgeResultRequest}
import judgeprotocol.objects.response.{JudgeTask, RegisterJudgerResponse}
import judger.config.AppConfig

import java.net.{URI, URLEncoder}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration

final case class LeaseExpiredException(message: String) extends RuntimeException(message)

final class JudgeHttpClient(httpClient: HttpClient, config: AppConfig):
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

  def heartbeat(judgerId: JudgerId): IO[Unit] =
    requestRaw(
      path = s"/api/worker/judge/judgers/${URLEncoder.encode(judgerId.value, StandardCharsets.UTF_8)}/heartbeat",
      method = "POST",
      body = Some(JudgerHeartbeatRequest().asJson.noSpaces)
    ).flatMap(handleHeartbeatResponse(judgerId, _))

  def claimTask(judgerId: JudgerId): IO[Option[JudgeTask]] =
    requestRaw(
      path = "/api/worker/judge/claim",
      method = "POST",
      body = Some(ClaimJudgeTaskRequest(judgerId).asJson.noSpaces)
    ).flatMap { response =>
      if response.statusCode == 204 then IO.pure(None)
      else if !response.isSuccess then
        requestFailed("Claim failed", response)
      else
        IO.fromEither(decode[JudgeTask](response.body).left.map(error => RuntimeException(error.getMessage))).map(Some(_))
    }

  def reportResult(submissionId: SubmissionId, result: ReportJudgeResultRequest): IO[Unit] =
    requestExpectSuccess(
      path = s"/api/worker/judge/submissions/${submissionId.value}/complete",
      method = "POST",
      body = Some(result.asJson.noSpaces)
    ).void

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
      else throw RuntimeException(s"Request failed with HTTP ${response.statusCode}: ${new String(response.body, StandardCharsets.UTF_8)}")
    }

  private def encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def handleHeartbeatResponse(judgerId: JudgerId, response: HttpResponse[String]): IO[Unit] =
    if response.isSuccess then IO.unit
    else if response.statusCode == 404 then IO.raiseError(LeaseExpiredException(s"Judger lease expired for ${judgerId.value}."))
    else requestFailed("Request failed", response)

  private def requestFailed[A](prefix: String, response: HttpResponse[String]): IO[A] =
    IO.raiseError(RuntimeException(s"$prefix with HTTP ${response.statusCode}: ${response.body}"))

object JudgeHttpClient:
  def create(config: AppConfig): JudgeHttpClient =
    new JudgeHttpClient(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
      config
    )

extension (response: HttpResponse[?])
  private def isSuccess: Boolean =
    response.statusCode >= 200 && response.statusCode < 300
