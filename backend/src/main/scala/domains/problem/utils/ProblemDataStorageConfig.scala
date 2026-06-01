package domains.problem.utils

final case class ProblemDataStorageConfig(
  minio: MinioProblemDataStorageConfig
)

final case class MinioProblemDataStorageConfig(
  endpoint: String,
  accessKey: String,
  secretKey: String,
  bucket: String,
  secure: Boolean
)

object ProblemDataStorageConfig:

  def loadFromEnvironment(): ProblemDataStorageConfig =
    fromEnvironment(sys.env)

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
