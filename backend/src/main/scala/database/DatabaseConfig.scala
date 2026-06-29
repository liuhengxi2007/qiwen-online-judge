package database

/** 数据库连接配置，来源于环境变量并用于创建 JDBC URL 和连接池。 */
final case class DatabaseConfig(
  host: String,
  port: Int,
  databaseName: String,
  user: String,
  password: String,
  maxPoolSize: Int,
  connectionTimeoutMs: Long
)

/** 数据库配置加载器和派生字段扩展。 */
object DatabaseConfig:

  private def defaultDatabaseName: String =
    sys.env
      .get("DB_NAME")
      .getOrElse(DatabaseDefaults.DefaultDatabaseName)

  extension (config: DatabaseConfig)
    /** 根据 host/port/databaseName 生成 PostgreSQL JDBC URL。 */
    def url: String =
      s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}"

  /** 从进程环境读取默认数据库配置，缺失时使用本地开发默认值。 */
  val default: DatabaseConfig =
    DatabaseConfig(
      host = sys.env.getOrElse("DB_HOST", "127.0.0.1"),
      port = sys.env.get("DB_PORT").flatMap(_.toIntOption).getOrElse(5432),
      databaseName = defaultDatabaseName,
      user = sys.env.getOrElse("DB_USER", "db"),
      password = requiredEnv("DB_PASSWORD"),
      maxPoolSize = sys.env.get("DB_MAX_POOL_SIZE").flatMap(_.toIntOption).getOrElse(10),
      connectionTimeoutMs = sys.env.get("DB_CONNECTION_TIMEOUT_MS").flatMap(_.toLongOption).getOrElse(3000L)
    )

  private def requiredEnv(name: String): String =
    sys.env.get(name).map(_.trim).filter(_.nonEmpty).getOrElse {
      throw IllegalStateException(s"$name must be configured.")
    }
