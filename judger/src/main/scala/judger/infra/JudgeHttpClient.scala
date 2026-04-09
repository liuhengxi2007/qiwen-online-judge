package judger.infra

import cats.effect.IO
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.model.*
import judger.config.AppConfig

import java.net.{URI, URLEncoder}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration

final class JudgeHttpClient(httpClient: HttpClient, config: AppConfig):
  def registerJudger: IO[RegisterJudgerResponse] =
    val body = RegisterJudgerRequest(
      preferredPrefix = config.preferredJudgerPrefix,
      host = config.host,
      processId = config.processId,
      supportedLanguages = config.supportedLanguages
    ).asJson.noSpaces
    requestExpectJson[RegisterJudgerResponse](
      path = "/api/internal/judgers/register",
      method = "POST",
      body = Some(body)
    )

  def heartbeat(judgerId: JudgerId): IO[Unit] =
    requestExpectSuccess(
      path = s"/api/internal/judgers/${URLEncoder.encode(judgerId.value, StandardCharsets.UTF_8)}/heartbeat",
      method = "POST",
      body = Some(JudgerHeartbeatRequest().asJson.noSpaces)
    ).void

  def claimTask(judgerId: JudgerId): IO[Option[JudgeTask]] =
    val body = ClaimJudgeTaskRequest(judgerId).asJson.noSpaces
    requestRaw(
      path = "/api/internal/judge/claim",
      method = "POST",
      body = Some(body)
    ).flatMap { response =>
      if response.statusCode == 204 then IO.pure(None)
      else if response.statusCode / 100 != 2 then
        IO.raiseError(RuntimeException(s"Claim failed with HTTP ${response.statusCode}: ${response.body}"))
      else
        IO.fromEither(decode[JudgeTask](response.body).left.map(error => RuntimeException(error.getMessage))).map(Some(_))
    }

  def reportResult(submissionId: SubmissionId, result: ReportJudgeResultRequest): IO[Unit] =
    requestExpectSuccess(
      path = s"/api/internal/judge/submissions/${submissionId.value}/complete",
      method = "POST",
      body = Some(result.asJson.noSpaces)
    ).void

  private def requestExpectSuccess(path: String, method: String, body: Option[String]): IO[String] =
    requestRaw(path, method, body).flatMap { response =>
      if response.statusCode / 100 == 2 then IO.pure(response.body)
      else IO.raiseError(RuntimeException(s"Request failed with HTTP ${response.statusCode}: ${response.body}"))
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

object JudgeHttpClient:
  def create(config: AppConfig): JudgeHttpClient =
    new JudgeHttpClient(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
      config
    )
