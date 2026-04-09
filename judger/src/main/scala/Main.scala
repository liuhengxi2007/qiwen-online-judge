import cats.effect.{IO, IOApp}
import judger.application.JudgerService
import judger.config.{AppConfig, RegisteredJudger}
import judger.infra.JudgeHttpClient
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationLong

object Main extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    AppConfig.fromEnvironment(sys.env) match
      case Left(message) =>
        IO.raiseError(IllegalArgumentException(message))
      case Right(config) =>
        val httpClient = JudgeHttpClient.create(config)
        for
          registration <- httpClient.registerJudger
          registeredJudger = RegisteredJudger.fromResponse(registration)
          service = JudgerService(config, registeredJudger, httpClient, logger)
          _ <- logger.info(
            s"Starting judger ${registeredJudger.judgerId.value} against ${config.backendBaseUrl} " +
              s"(prefix=${config.preferredJudgerPrefix.value}, host=${config.host})"
          )
          heartbeatFiber <- heartbeatLoop(httpClient, registeredJudger).start
          result <- service.runForever.guarantee(heartbeatFiber.cancel)
        yield result

  private def heartbeatLoop(httpClient: JudgeHttpClient, registeredJudger: RegisteredJudger): IO[Nothing] =
    (httpClient.heartbeat(registeredJudger.judgerId) *> IO.sleep(registeredJudger.heartbeatIntervalMs.millis)).foreverM
