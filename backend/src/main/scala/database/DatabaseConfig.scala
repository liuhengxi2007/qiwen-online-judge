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
      /** FIXME-CN: 缺失 DB_PASSWORD 时默认使用 root，生产环境误配置时可能以弱默认密码启动。 */
      password = sys.env.getOrElse("DB_PASSWORD", "root"),
      maxPoolSize = sys.env.get("DB_MAX_POOL_SIZE").flatMap(_.toIntOption).getOrElse(10),
      connectionTimeoutMs = sys.env.get("DB_CONNECTION_TIMEOUT_MS").flatMap(_.toLongOption).getOrElse(3000L)
    )
