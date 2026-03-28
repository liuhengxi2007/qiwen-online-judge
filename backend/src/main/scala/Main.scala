import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import database.DatabaseSession
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, Logger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.ApiRouter
import tables.AuthUserTable
import tables.NoteTable

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

  private val applicationResource: cats.effect.Resource[IO, Server] =
    for
      _ <- DatabaseSession.initialize
      _ <- cats.effect.Resource.eval {
        DatabaseSession.withTransactionConnection { connection =>
          for
            _ <- logger.info("Initializing database schema")
            _ <- AuthUserTable.initialize(connection)
            _ <- NoteTable.initialize(connection)
          yield ()
        }
      }
      server <- serverResource
    yield server

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- applicationResource.useForever
    yield ()
