package domains.user.utils

import cats.effect.IO
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}

import java.io.ByteArrayInputStream

/** 基于 MinIO 的用户头像对象存储实现，负责桶存在性检查和对象读写。 */
final class MinioUserAvatarStorage(config: MinioUserAvatarStorageConfig) extends UserAvatarStorage:

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

  /** 确保桶存在后读取头像对象，MinIO 报对象缺失时返回 None。 */
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
        /** FIXME-CN: 这里把所有 MinIO ErrorResponseException 都当作对象缺失，可能隐藏权限、桶配置或服务端错误。 */
        case _: io.minio.errors.ErrorResponseException =>
          None
    }

  /** 删除头像对象；删除失败被吞掉，避免旧头像清理影响主流程。 */
  override def deleteObject(objectKey: String): IO[Unit] =
    /** FIXME-CN: 删除失败完全吞掉，可能长期保留孤儿头像对象且没有日志可追踪。 */
    ensureBucket() *> IO.blocking {
      client.removeObject(
        RemoveObjectArgs
          .builder()
          .bucket(config.bucket)
          .`object`(objectKey)
          .build()
      )
      ()
    }.handleError(_ => ())

  private def ensureBucket(): IO[Unit] =
    IO.blocking {
      val exists = client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())
      if !exists then
        client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
    }
