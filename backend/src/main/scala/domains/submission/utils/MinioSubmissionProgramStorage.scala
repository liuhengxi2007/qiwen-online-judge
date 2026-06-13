package domains.submission.utils

import cats.effect.IO
import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}

import java.io.ByteArrayInputStream

/** 基于 MinIO 的提交程序源码存储；sourceKey 已包含提交 UUID 和程序角色。 */
final class MinioSubmissionProgramStorage(config: MinioSubmissionProgramStorageConfig) extends SubmissionProgramStorage:

  private val client =
    MinioClient
      .builder()
      .endpoint(config.endpoint)
      .credentials(config.accessKey, config.secretKey)
      .build()

  /** 写入单个源码对象；源码按 UTF-8 编码保存。 */
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

  /** 读取单个源码对象；对象不存在返回 None，内容非法视为存储损坏。 */
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
        // FIXME-CN: 这里把所有 MinIO ErrorResponseException 都当成源码不存在；权限、桶状态或服务端错误会被静默映射为 None。
        case _: io.minio.errors.ErrorResponseException =>
          None
    }

  /** 删除单个源码对象；先读后删以返回对象是否存在。 */
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
