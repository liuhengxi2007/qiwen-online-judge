package database

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.Connection

/** 数据库会话门面，封装 Hikari 连接池和事务边界。 */
final class DatabaseSession private (dataSource: HikariDataSource):

  /** 使用池化连接执行事务，成功提交，失败回滚并重新抛出原始错误。 */
  def withTransactionConnection[A](operation: Connection => IO[A]): IO[A] =
    pooledConnectionResource.use { connection =>
      for
        _ <- IO.blocking(connection.setAutoCommit(false))
        result <- operation(connection).attempt
        _ <- result match
          case Right(_) => IO.blocking(connection.commit())
          case Left(_) => IO.blocking(connection.rollback()).handleErrorWith(_ => IO.unit)
        value <- IO.fromEither(result)
      yield value
    }

  private def pooledConnectionResource: Resource[IO, Connection] =
    Resource.make(IO.blocking(dataSource.getConnection)) { connection =>
      IO.blocking(connection.close()).handleErrorWith(_ => IO.unit)
    }

/** 创建和关闭数据库连接池的资源入口。 */
object DatabaseSession:

  private val logger = Slf4jLogger.getLogger[IO]
  private val config = DatabaseConfig.default

  /** 构造数据库会话资源，获取时初始化 PostgreSQL 连接池，释放时关闭池。 */
  def resource: Resource[IO, DatabaseSession] =
    Resource
      .make(createDataSource)(closeDataSource)
      .map(dataSource => new DatabaseSession(dataSource))

  private def createDataSource: IO[HikariDataSource] =
    IO.blocking {
      Class.forName("org.postgresql.Driver")
      val hikariConfig = HikariConfig()
      hikariConfig.setJdbcUrl(config.url)
      hikariConfig.setUsername(config.user)
      hikariConfig.setPassword(config.password)
      hikariConfig.setMaximumPoolSize(config.maxPoolSize)
      hikariConfig.setConnectionTimeout(config.connectionTimeoutMs)
      hikariConfig.setPoolName("qiwen-online-judge-backend-pool")
      new HikariDataSource(hikariConfig)
    }.flatTap(_ =>
      logger.info(
        s"Initialized PostgreSQL connection pool, host=${config.host}, port=${config.port}, database=${config.databaseName}, maxPoolSize=${config.maxPoolSize}"
      )
    )

  private def closeDataSource(dataSource: HikariDataSource): IO[Unit] =
    IO.blocking(dataSource.close()).handleErrorWith(_ => IO.unit)
