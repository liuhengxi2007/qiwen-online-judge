package domains.submission.utils

import cats.effect.IO
import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}

import java.io.ByteArrayInputStream

final class MinioSubmissionProgramStorage(config: MinioSubmissionProgramStorageConfig) extends SubmissionProgramStorage:

  private val client =
    MinioClient
      .builder()
      .endpoint(config.endpoint)
      .credentials(config.accessKey, config.secretKey)
      .build()

  override def writeSource(sourceKey: String, sourceCode: SubmissionSourceCode): IO[Unit] =
    val bytes = SubmissionProgramManifest.sourceBytes(sourceCode)
    ensureBucket() *> IO.blocking {
      client.putObject(
        PutObjectArgs
          .builder()
          .bucket(config.bucket)
          .`object`(sourceKey)
          .stream(ByteArrayInputStream(bytes), bytes.length.toLong, -1)
          .contentType("text/plain; charset=utf-8")
          .build()
      )
      ()
    }

  override def readSource(sourceKey: String): IO[Option[SubmissionSourceCode]] =
    ensureBucket() *> IO.blocking {
      try
        val inputStream = client.getObject(
          GetObjectArgs
            .builder()
            .bucket(config.bucket)
            .`object`(sourceKey)
            .build()
        )
        try
          Some(
            SubmissionProgramStorage
              .sourceCodeFromBytes(inputStream.readAllBytes())
              .fold(message => throw IllegalStateException(s"Invalid stored submission source: $message"), identity)
          )
        finally inputStream.close()
      catch
        case _: io.minio.errors.ErrorResponseException =>
          None
    }

  override def deleteSource(sourceKey: String): IO[Boolean] =
    readSource(sourceKey).flatMap {
      case None => IO.pure(false)
      case Some(_) =>
        ensureBucket() *> IO.blocking {
          client.removeObject(
            RemoveObjectArgs
              .builder()
              .bucket(config.bucket)
              .`object`(sourceKey)
              .build()
          )
          true
        }
    }

  private def ensureBucket(): IO[Unit] =
    IO.blocking {
      val exists = client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())
      if !exists then
        client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
    }
