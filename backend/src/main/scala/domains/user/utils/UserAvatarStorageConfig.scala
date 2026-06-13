package domains.user.utils

/** 用户头像存储配置，当前仅支持 MinIO 后端。 */
final case class UserAvatarStorageConfig(
  minio: MinioUserAvatarStorageConfig
)

/** MinIO 头像存储连接配置，来源于环境变量。 */
final case class MinioUserAvatarStorageConfig(
  endpoint: String,
  accessKey: String,
  secretKey: String,
  bucket: String,
  secure: Boolean
)

/** 用户头像存储配置加载器，负责校验必需环境变量。 */
object UserAvatarStorageConfig:

  /** 从进程环境加载头像存储配置，缺失必需变量时抛出异常。 */
  def loadFromEnvironment(): UserAvatarStorageConfig =
    fromEnvironment(sys.env)

  /** 从指定环境映射加载头像存储配置，便于测试或自定义启动环境。 */
  def fromEnvironment(env: scala.collection.Map[String, String]): UserAvatarStorageConfig =
    val values = requiredMinioValues(env)
    UserAvatarStorageConfig(
      minio = MinioUserAvatarStorageConfig(
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
        s"User avatar storage requires MinIO configuration. Missing environment variable(s): ${missing.mkString(", ")}."
      )
    values.collect { case (name, Some(value)) => name -> value }.toMap
