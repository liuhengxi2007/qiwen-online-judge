import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s.{host, port}
import database.DatabaseSession
import database.table.resource_access_grant.ResourceAccessGrantTable
import domains.auth.table.auth_account.{AuthAccountTable, AuthAccountTableSupport}
import domains.auth.table.session.SessionTable
import domains.auth.utils.{PasswordHasher, RedisSessionCache, SessionCache, SessionCacheConfig, SessionConfig, SessionStore}
import domains.blog.table.blog.BlogTable
import domains.judge.utils.JudgeConfig
import domains.judger.table.judger.JudgerTable
import domains.message.table.message.MessageTable
import domains.message.utils.MessageEventHub
import domains.notification.table.notification.NotificationTable
import domains.notification.utils.NotificationEventHub
import domains.problem.table.problem.ProblemTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problem.utils.{LocalProblemDataStorage, MinioProblemDataStorage, ProblemDataStorage, ProblemDataStorageBackend, ProblemDataStorageConfig}
import domains.problemset.table.problem_set.ProblemSetTable
import domains.submission.table.submission.SubmissionTable
import domains.submission.utils.{LocalSubmissionProgramStorage, MinioSubmissionProgramStorage, SubmissionProgramStorage, SubmissionProgramStorageBackend, SubmissionProgramStorageConfig}
import domains.user.table.user_profile.UserProfileTable
import domains.usergroup.table.user_group.UserGroupTable
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.ApiRouter

object Main extends IOApp.Simple:

  private val logger = Slf4jLogger.getLogger[IO]

  private def serverResource(httpApp: HttpApp[IO]): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build

  private def problemDataStorageResource(config: ProblemDataStorageConfig): Resource[IO, ProblemDataStorage] =
    Resource.eval {
      IO.delay {
        config.backend match
          case ProblemDataStorageBackend.Local =>
            LocalProblemDataStorage(config.localRootDirectory)
          case ProblemDataStorageBackend.Minio =>
            config.minio match
              case Some(minioConfig) => MinioProblemDataStorage(minioConfig)
              case None => throw IllegalStateException("MinIO storage backend requires MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET.")
      }
    }

  private def submissionProgramStorageResource(config: SubmissionProgramStorageConfig): Resource[IO, SubmissionProgramStorage] =
    Resource.eval {
      IO.delay {
        config.backend match
          case SubmissionProgramStorageBackend.Local =>
            LocalSubmissionProgramStorage(config.localRootDirectory)
          case SubmissionProgramStorageBackend.Minio =>
            config.minio match
              case Some(minioConfig) => MinioSubmissionProgramStorage(minioConfig)
              case None => throw IllegalStateException("MinIO submission program storage requires MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET.")
      }
    }

  private val resource: Resource[IO, Server] =
    for
      databaseSession <- DatabaseSession.resource
      sessionCache <- SessionCacheConfig
        .fromEnvironment(sys.env)
        .map(RedisSessionCache.resource)
        .getOrElse(Resource.pure[IO, SessionCache](SessionCache.noop))
      sessionStore <- Resource.eval(SessionStore.create(databaseSession, sessionCache))
      messageEventHub <- MessageEventHub.resource
      notificationEventHub <- NotificationEventHub.resource
      seedAdminPasswordHash <- Resource.eval(
        PasswordHasher.hashPassword(AuthAccountTableSupport.seedAdminPlaintextPassword)
      )
      judgeConfig = JudgeConfig.loadFromEnvironment()
      problemDataStorageConfig = ProblemDataStorageConfig.loadFromEnvironment()
      submissionProgramStorageConfig = SubmissionProgramStorageConfig.loadFromEnvironment()
      problemDataStorage <- problemDataStorageResource(problemDataStorageConfig)
      submissionProgramStorage <- submissionProgramStorageResource(submissionProgramStorageConfig)
      _ <- Resource.eval {
        databaseSession.withTransactionConnection { connection =>
          for
            _ <- logger.info("Initializing database schema")
            _ <- AuthAccountTable.initialize(connection, seedAdminPasswordHash)
            _ <- UserProfileTable.initialize(connection)
            _ <- SessionTable.initialize(connection, SessionConfig.default.ttl)
            _ <- ProblemTable.initialize(connection)
            _ <- ProblemDataFileTable.initialize(connection)
            _ <- ProblemSetTable.initialize(connection)
            _ <- SubmissionTable.initialize(connection, submissionProgramStorage)
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
          ApiRouter.httpApp(databaseSession, sessionStore, judgeConfig, problemDataStorage, submissionProgramStorage, messageEventHub, notificationEventHub)
        )
      server <- serverResource(httpApp)
    yield server

  override def run: IO[Unit] =
    for
      _ <- logger.info("Starting backend-sample on http://0.0.0.0:8080")
      _ <- resource.useForever
    yield ()
