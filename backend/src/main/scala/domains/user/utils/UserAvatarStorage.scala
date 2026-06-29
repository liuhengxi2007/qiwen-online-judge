package domains.user.utils

import cats.effect.IO
import io.minio.errors.ErrorResponseException
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shared.application.upload.MinioErrorHandling

import java.io.ByteArrayInputStream

/** 用户头像对象存储上下文，携带 MinIO/S3 配置和共享 client。 */
final case class UserAvatarStorageContext(
  config: MinioUserAvatarStorageConfig,
  client: MinioClient
)

/** 用户头像存储函数集合，负责桶存在性检查和对象读写。 */
object UserAvatarStorage:

  private val logger = Slf4jLogger.getLogger[IO]

  /** 创建 MinIO-backed 存储上下文；client 在进程生命周期内复用。 */
  def create(config: MinioUserAvatarStorageConfig): UserAvatarStorageContext =
    UserAvatarStorageContext(
      config = config,
      client = MinioClient
        .builder()
        .endpoint(config.endpoint)
        .credentials(config.accessKey, config.secretKey)
        .build()
    )

  /** 确保桶存在后上传头像对象，副作用为写入 MinIO。 */
  def writeObject(context: UserAvatarStorageContext, objectKey: String, bytes: Array[Byte], contentType: String): IO[Unit] =
    ensureBucket(context) *> IO.blocking {
      context.client.putObject(
        PutObjectArgs
          .builder()
          .bucket(context.config.bucket)
          .`object`(objectKey)
          .stream(ByteArrayInputStream(bytes), bytes.length.toLong, -1)
          .contentType(contentType)
          .build()
      )
      ()
    }

  /** 确保桶存在后读取头像对象，MinIO 报对象缺失时返回 None，其它存储错误继续暴露。 */
  def readObject(context: UserAvatarStorageContext, objectKey: String): IO[Option[Array[Byte]]] =
    ensureBucket(context) *> IO.blocking {
      try
        val inputStream = context.client.getObject(
          GetObjectArgs
            .builder()
            .bucket(context.config.bucket)
            .`object`(objectKey)
            .build()
        )
        try Some(inputStream.readAllBytes())
        finally inputStream.close()
      catch
        case error: ErrorResponseException if MinioErrorHandling.isObjectNotFound(error) =>
          None
    }

  /** 删除头像对象；删除失败会记录日志但不影响主流程。 */
  def deleteObject(context: UserAvatarStorageContext, objectKey: String): IO[Unit] =
    ensureBucket(context) *> IO.blocking {
      context.client.removeObject(
        RemoveObjectArgs
          .builder()
          .bucket(context.config.bucket)
          .`object`(objectKey)
          .build()
      )
      ()
    }.handleErrorWith(error => logger.warn(error)(s"Failed to delete user avatar object: $objectKey"))

  private def ensureBucket(context: UserAvatarStorageContext): IO[Unit] =
    IO.blocking {
      val exists = context.client.bucketExists(BucketExistsArgs.builder().bucket(context.config.bucket).build())
      if !exists then
        context.client.makeBucket(MakeBucketArgs.builder().bucket(context.config.bucket).build())
    }
