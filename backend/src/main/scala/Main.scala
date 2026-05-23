import domains.auth.application.{RedisSessionCache, SessionCache, SessionCacheConfig, SessionStore}
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import database.DatabaseSession
import database.table.resource_access_grant.ResourceAccessGrantTable
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.slf4j.Slf4jLogger
import domains.system.http.ApiRouter
import domains.auth.table.auth_user.AuthUserTable
import domains.blog.table.blog.BlogTable
import domains.judge.application.JudgeConfig
import domains.problem.application.{LocalProblemDataStorage, MinioProblemDataStorage, ProblemDataStorage, ProblemDataStorageBackend, ProblemDataStorageConfig}
import domains.auth.table.session.SessionTable
import domains.problem.table.problem.ProblemTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problemset.table.problem_set.ProblemSetTable
import domains.submission.table.submission.SubmissionTable
import domains.judger.table.judger.JudgerTable
import domains.message.application.MessageEventHub
import domains.message.table.message.MessageTable
import domains.notification.application.NotificationEventHub
import domains.notification.table.notification.NotificationTable
import domains.usergroup.table.user_group.UserGroupTable

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
      sessionCache <- SessionCacheConfig
        .fromEnvironment(sys.env)
        .map(RedisSessionCache.resource)
        .getOrElse(cats.effect.Resource.pure[IO, SessionCache](SessionCache.noop))
      sessionStore <- cats.effect.Resource.eval(SessionStore.create(databaseSession, sessionCache))
      messageEventHub <- MessageEventHub.resource
      notificationEventHub <- NotificationEventHub.resource
      seedAdminPasswordHash <- cats.effect.Resource.eval(
        domains.auth.application.PasswordHasher.hashPassword(domains.auth.table.auth_user.AuthUserTableSupport.seedAdminPlaintextPassword)
      )
      judgeConfig = JudgeConfig.loadFromEnvironment()
      problemDataStorageConfig = ProblemDataStorageConfig.loadFromEnvironment()
      problemDataStorage <- cats.effect.Resource.eval {
        IO.delay {
          problemDataStorageConfig.backend match
            case ProblemDataStorageBackend.Local =>
              LocalProblemDataStorage(problemDataStorageConfig.localRootDirectory)
            case ProblemDataStorageBackend.Minio =>
              problemDataStorageConfig.minio match
                case Some(config) => MinioProblemDataStorage(config)
                case None => throw IllegalStateException("MinIO storage backend requires MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET.")
        }
      }
      _ <- cats.effect.Resource.eval {
        databaseSession.withTransactionConnection { connection =>
          for
            _ <- logger.info("Initializing database schema")
            _ <- AuthUserTable.initialize(connection, seedAdminPasswordHash)
            _ <- SessionTable.initialize(connection, domains.auth.application.SessionConfig.default.ttl)
            _ <- ProblemTable.initialize(connection)
            _ <- ProblemDataFileTable.initialize(connection)
            _ <- ProblemSetTable.initialize(connection)
            _ <- SubmissionTable.initialize(connection)
            _ <- BlogTable.initialize(connection)
            _ <- JudgerTable.initialize(connection)
            _ <- UserGroupTable.initialize(connection)
            _ <- MessageTable.initialize(connection)
            _ <- NotificationTable.initialize(connection)
            _ <- ResourceAccessGrantTable.initialize(connection)
          yield ()
        }
      }
      httpApp =
        CORS.policy.withAllowOriginAll(
          ApiRouter.httpApp(databaseSession, sessionStore, judgeConfig, problemDataStorage, messageEventHub, notificationEventHub)
        )
      server <- serverResource(httpApp)
    yield server

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- applicationResource.useForever
    yield ()
