import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, Logger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.ApiRouter

object Main extends IOApp.Simple:

  private val logger = Slf4jLogger.getLogger[IO]

  private val httpApp =
    CORS.policy.withAllowOriginAll(
      Logger.httpApp(logHeaders = true, logBody = false)(ApiRouter.httpApp)
    )

  private val serverResource: cats.effect.Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- serverResource.useForever
    yield ()
