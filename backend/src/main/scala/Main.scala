import domains.auth.application.SessionStore
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import database.DatabaseSession
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, Logger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import domains.system.http.ApiRouter
import domains.auth.table.AuthUserTable
import domains.judge.application.JudgeConfig
import domains.auth.table.SessionTable
import domains.problem.table.ProblemTable
import domains.problemset.table.ProblemSetTable
import domains.shared.access.ResourceViewerGrantTable
import domains.submission.table.SubmissionTable
import domains.judger.table.JudgerTable
import domains.usergroup.table.UserGroupTable

object Main extends IOApp.Simple:

  private val logger = Slf4jLogger.getLogger[IO]

  private def serverResource(httpApp: org.http4s.HttpApp[IO]): cats.effect.Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build

  private val applicationResource: cats.effect.Resource[IO, Server] =
    for
      databaseSession <- DatabaseSession.resource
      sessionStore <- cats.effect.Resource.eval(SessionStore.create(databaseSession))
      judgeConfig = JudgeConfig.loadFromEnvironment()
      _ <- cats.effect.Resource.eval {
        databaseSession.withTransactionConnection { connection =>
          for
            _ <- logger.info("Initializing database schema")
            _ <- AuthUserTable.initialize(connection)
            _ <- SessionTable.initialize(connection, domains.auth.application.SessionConfig.default.ttl)
            _ <- ProblemTable.initialize(connection)
            _ <- ProblemSetTable.initialize(connection)
            _ <- SubmissionTable.initialize(connection)
            _ <- JudgerTable.initialize(connection)
            _ <- UserGroupTable.initialize(connection)
            _ <- ResourceViewerGrantTable.initialize(connection)
          yield ()
        }
      }
      httpApp = CORS.policy.withAllowOriginAll(
        Logger.httpApp(logHeaders = true, logBody = false)(ApiRouter.httpApp(databaseSession, sessionStore, judgeConfig))
      )
      server <- serverResource(httpApp)
    yield server

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- applicationResource.useForever
    yield ()
