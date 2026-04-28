import cats.effect.{IO, IOApp}
import cats.effect.kernel.Ref
import judger.application.JudgerService
import judger.config.{AppConfig, RegisteredJudger}
import judger.infra.{JudgeHttpClient, LeaseExpiredException, ProblemDataCache}
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
        val problemDataCache = ProblemDataCache(config, httpClient)
        for
          registeredJudger <- registerJudger(httpClient)
          registeredJudgerRef <- Ref.of[IO, RegisteredJudger](registeredJudger)
          service = JudgerService(config, registeredJudgerRef, httpClient, problemDataCache, logger)
          _ <- logger.info(
            s"Starting judger ${registeredJudger.judgerId.value} against ${config.backendBaseUrl} " +
              s"(prefix=${config.preferredJudgerPrefix.value}, host=${config.host})"
          )
          heartbeatFiber <- heartbeatLoop(httpClient, registeredJudgerRef).start
          result <- service.runForever.guarantee(heartbeatFiber.cancel)
        yield result

  private def heartbeatLoop(httpClient: JudgeHttpClient, registeredJudgerRef: Ref[IO, RegisteredJudger]): IO[Nothing] =
    heartbeatIteration(httpClient, registeredJudgerRef).foreverM

  private def heartbeatIteration(httpClient: JudgeHttpClient, registeredJudgerRef: Ref[IO, RegisteredJudger]): IO[Unit] =
    registeredJudgerRef.get.flatMap { registeredJudger =>
      httpClient
        .heartbeat(registeredJudger.judgerId)
        .handleErrorWith {
          case LeaseExpiredException(_) =>
            for
              _ <- logger.warn(s"Judger lease expired for ${registeredJudger.judgerId.value}; re-registering.")
              renewedJudger <- registerJudger(httpClient)
              _ <- registeredJudgerRef.set(renewedJudger)
              _ <- logger.info(s"Re-registered judger as ${renewedJudger.judgerId.value}.")
            yield ()
          case error =>
            logger.error(error)(s"Heartbeat failed for ${registeredJudger.judgerId.value}.")
        } *> IO.sleep(registeredJudger.heartbeatIntervalMs.millis)
    }

  private def registerJudger(httpClient: JudgeHttpClient): IO[RegisteredJudger] =
    httpClient.registerJudger.map(RegisteredJudger.fromResponse)
