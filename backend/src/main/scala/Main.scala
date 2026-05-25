import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import server.ApplicationResources

object Main extends IOApp.Simple:

  private val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- ApplicationResources.resource.useForever
    yield ()
