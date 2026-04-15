import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import database.DatabaseSession
import routes.ApiRouter
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple:

  private val logger = Slf4jLogger.getLogger[IO]

  private val httpApp =
    Logger.httpApp(logHeaders = true, logBody = false)(ApiRouter.httpApp)

  private val serverResource: cats.effect.Resource[IO, Server] =
    for
      _ <- DatabaseSession.initialize
      server <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
    yield server

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- serverResource.useForever
    yield ()
