package judger.infra

import cats.effect.IO
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.model.{ClaimJudgeTaskRequest, JudgeTask, ReportJudgeResultRequest, SubmissionId}
import judger.config.AppConfig

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration

final class JudgeHttpClient(httpClient: HttpClient, config: AppConfig):
  def claimTask: IO[Option[JudgeTask]] =
    val body = ClaimJudgeTaskRequest(config.judgerName).asJson.noSpaces
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
