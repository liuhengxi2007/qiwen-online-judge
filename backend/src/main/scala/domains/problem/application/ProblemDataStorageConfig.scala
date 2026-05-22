package domains.problem.application



import java.nio.file.{Path, Paths}

final case class ProblemDataStorageConfig(
  backend: ProblemDataStorageBackend,
  localRootDirectory: Path,
  minio: Option[MinioProblemDataStorageConfig]
)

enum ProblemDataStorageBackend:
  case Local
  case Minio

final case class MinioProblemDataStorageConfig(
  endpoint: String,
  accessKey: String,
  secretKey: String,
  bucket: String,
  secure: Boolean
)

object ProblemDataStorageConfig:

  def loadFromEnvironment(): ProblemDataStorageConfig =
    val backend = sys.env.get("PROBLEM_DATA_STORAGE_BACKEND").map(_.trim.toLowerCase) match
      case Some("minio") => ProblemDataStorageBackend.Minio
      case _ => ProblemDataStorageBackend.Local

    val localRootDirectory =
      sys.env
        .get("PROBLEM_DATA_LOCAL_ROOT")
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(Paths.get(_))
        .getOrElse(Paths.get(sys.props.getOrElse("user.dir", "."), "problems"))

    val minioConfig =
      for
        endpoint <- sys.env.get("MINIO_ENDPOINT").map(_.trim).filter(_.nonEmpty)
        accessKey <- sys.env.get("MINIO_ACCESS_KEY").map(_.trim).filter(_.nonEmpty)
        secretKey <- sys.env.get("MINIO_SECRET_KEY").map(_.trim).filter(_.nonEmpty)
        bucket <- sys.env.get("MINIO_BUCKET").map(_.trim).filter(_.nonEmpty)
      yield MinioProblemDataStorageConfig(
        endpoint = endpoint,
        accessKey = accessKey,
        secretKey = secretKey,
        bucket = bucket,
        secure = sys.env.get("MINIO_SECURE").forall(_.trim.toLowerCase != "false")
      )

    ProblemDataStorageConfig(
      backend = backend,
      localRootDirectory = localRootDirectory,
      minio = minioConfig
    )
