package domains.problem.utils

/** 题目数据存储总配置；当前实现只包含 MinIO 后端配置。 */
final case class ProblemDataStorageConfig(
  minio: MinioProblemDataStorageConfig
)

/** MinIO 题目数据存储配置；由环境变量加载，secretKey 不应写入日志。 */
final case class MinioProblemDataStorageConfig(
  endpoint: String,
  accessKey: String,
  secretKey: String,
  bucket: String,
  secure: Boolean
)

/** 题目数据存储配置加载器；缺失必需环境变量时会在启动阶段失败。 */
object ProblemDataStorageConfig:

  /** 从进程环境加载题目数据存储配置。 */
  def loadFromEnvironment(): ProblemDataStorageConfig =
    fromEnvironment(sys.env)

  /** 从传入环境映射构造配置，便于测试或不同启动入口复用。 */
  def fromEnvironment(env: scala.collection.Map[String, String]): ProblemDataStorageConfig =
    val values = requiredMinioValues(env)
    ProblemDataStorageConfig(
      minio = MinioProblemDataStorageConfig(
        endpoint = values("MINIO_ENDPOINT"),
        accessKey = values("MINIO_ACCESS_KEY"),
        secretKey = values("MINIO_SECRET_KEY"),
        bucket = values("MINIO_BUCKET"),
        secure = env.get("MINIO_SECURE").forall(_.trim.toLowerCase != "false")
      )
    )

  private val requiredMinioEnvironmentVariables =
    List("MINIO_ENDPOINT", "MINIO_ACCESS_KEY", "MINIO_SECRET_KEY", "MINIO_BUCKET")

  private def requiredMinioValues(env: scala.collection.Map[String, String]): Map[String, String] =
    val values = requiredMinioEnvironmentVariables.map { name =>
      name -> env.get(name).map(_.trim).filter(_.nonEmpty)
    }
    val missing = values.collect { case (name, None) => name }
    if missing.nonEmpty then
      throw IllegalStateException(
        s"Problem data storage requires MinIO configuration. Missing environment variable(s): ${missing.mkString(", ")}."
      )
    values.collect { case (name, Some(value)) => name -> value }.toMap
