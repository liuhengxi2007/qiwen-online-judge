package domains.user.utils

import cats.effect.IO
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}

import java.io.ByteArrayInputStream

final class MinioUserAvatarStorage(config: MinioUserAvatarStorageConfig) extends UserAvatarStorage:

  private val client =
    MinioClient
      .builder()
      .endpoint(config.endpoint)
      .credentials(config.accessKey, config.secretKey)
      .build()

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
        case _: io.minio.errors.ErrorResponseException =>
          None
    }

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
    }.handleError(_ => ())

  private def ensureBucket(): IO[Unit] =
    IO.blocking {
      val exists = client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())
      if !exists then
        client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
    }
