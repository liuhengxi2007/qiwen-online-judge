import cats.effect.{IO, IOApp}
import judger.application.JudgerService
import judger.config.AppConfig
import judger.infra.JudgeHttpClient
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple:
  private val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    AppConfig.fromEnvironment(sys.env) match
      case Left(message) =>
        IO.raiseError(IllegalArgumentException(message))
      case Right(config) =>
        val httpClient = JudgeHttpClient.create(config)
        val service = JudgerService(config, httpClient, logger)
        logger.info(s"Starting judger ${config.judgerName.value} against ${config.backendBaseUrl}") *>
          service.runForever
