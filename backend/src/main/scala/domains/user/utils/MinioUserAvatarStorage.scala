package domains.user.utils

import cats.effect.IO
import io.minio.errors.ErrorResponseException
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shared.utils.MinioErrorHandling

import java.io.ByteArrayInputStream

/** 基于 MinIO 的用户头像对象存储实现，负责桶存在性检查和对象读写。 */
final class MinioUserAvatarStorage(config: MinioUserAvatarStorageConfig) extends UserAvatarStorage:

  private val logger = Slf4jLogger.getLogger[IO]

  private val client =
    MinioClient
      .builder()
      .endpoint(config.endpoint)
      .credentials(config.accessKey, config.secretKey)
      .build()

  /** 确保桶存在后上传头像对象，副作用为写入 MinIO。 */
  override def writeObject(objectKey: String, bytes: Array[Byte], contentType: String): IO[Unit] =
    ensureBucket() *> IO.blocking {
      client.putObject(
        PutObjectArgs
          .builder()
          .bucket(config.bucket)
          .`object`(objectKey)
          .stream(ByteArrayInputStream(bytes), bytes.length.toLong, -1)
          .contentType(contentType)
          .build()
      )
      ()
    }

  /** 确保桶存在后读取头像对象，MinIO 报对象缺失时返回 None，其它存储错误继续暴露。 */
  override def readObject(objectKey: String): IO[Option[Array[Byte]]] =
    ensureBucket() *> IO.blocking {
      try
        val inputStream = client.getObject(
          GetObjectArgs
            .builder()
            .bucket(config.bucket)
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
  override def deleteObject(objectKey: String): IO[Unit] =
    ensureBucket() *> IO.blocking {
      client.removeObject(
        RemoveObjectArgs
          .builder()
          .bucket(config.bucket)
          .`object`(objectKey)
          .build()
      )
      ()
    }.handleErrorWith(error => logger.warn(error)(s"Failed to delete user avatar object: $objectKey"))

  private def ensureBucket(): IO[Unit] =
    IO.blocking {
      val exists = client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())
      if !exists then
        client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
    }
