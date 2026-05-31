package domains.submission.utils

import java.nio.file.{Path, Paths}

final case class SubmissionProgramStorageConfig(
  backend: SubmissionProgramStorageBackend,
  localRootDirectory: Path,
  minio: Option[MinioSubmissionProgramStorageConfig]
)

enum SubmissionProgramStorageBackend:
  case Local
  case Minio

final case class MinioSubmissionProgramStorageConfig(
  endpoint: String,
  accessKey: String,
  secretKey: String,
  bucket: String,
  secure: Boolean
)

object SubmissionProgramStorageConfig:

  def loadFromEnvironment(): SubmissionProgramStorageConfig =
    val backend = sys.env.get("SUBMISSION_PROGRAM_STORAGE_BACKEND").orElse(sys.env.get("PROBLEM_DATA_STORAGE_BACKEND")).map(_.trim.toLowerCase) match
      case Some("minio") => SubmissionProgramStorageBackend.Minio
      case _ => SubmissionProgramStorageBackend.Local

    val localRootDirectory =
      sys.env
        .get("SUBMISSION_PROGRAM_LOCAL_ROOT")
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(Paths.get(_))
        .getOrElse(Paths.get(sys.props.getOrElse("user.dir", "."), "submission-programs"))

    val minioConfig =
      for
        endpoint <- sys.env.get("MINIO_ENDPOINT").map(_.trim).filter(_.nonEmpty)
        accessKey <- sys.env.get("MINIO_ACCESS_KEY").map(_.trim).filter(_.nonEmpty)
        secretKey <- sys.env.get("MINIO_SECRET_KEY").map(_.trim).filter(_.nonEmpty)
        bucket <- sys.env.get("MINIO_BUCKET").map(_.trim).filter(_.nonEmpty)
      yield MinioSubmissionProgramStorageConfig(
        endpoint = endpoint,
        accessKey = accessKey,
        secretKey = secretKey,
        bucket = bucket,
        secure = sys.env.get("MINIO_SECURE").forall(_.trim.toLowerCase != "false")
      )

    SubmissionProgramStorageConfig(
      backend = backend,
      localRootDirectory = localRootDirectory,
      minio = minioConfig
    )
