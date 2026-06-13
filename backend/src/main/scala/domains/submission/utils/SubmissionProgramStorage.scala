package domains.submission.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest
import io.minio.errors.ErrorResponseException
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}
import shared.utils.MinioErrorHandling

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/** 提交程序源码存储上下文；sourceKey 由 manifest 生成。 */
final case class SubmissionProgramStorageContext(
  config: MinioSubmissionProgramStorageConfig,
  client: MinioClient
)

/** 提交程序存储函数集合；源码对象当前存放在 MinIO。 */
object SubmissionProgramStorage:

  /** 创建 MinIO-backed 存储上下文；client 在进程生命周期内复用。 */
  def create(config: MinioSubmissionProgramStorageConfig): SubmissionProgramStorageContext =
    SubmissionProgramStorageContext(
      config = config,
      client = MinioClient
        .builder()
        .endpoint(config.endpoint)
        .credentials(config.accessKey, config.secretKey)
        .build()
    )

  /** 写入单个源码对象；源码按 UTF-8 编码保存。 */
  def writeSource(context: SubmissionProgramStorageContext, sourceKey: String, sourceCode: SubmissionSourceCode): IO[Unit] =
    val bytes = SubmissionProgramManifest.sourceBytes(sourceCode)
    ensureBucket(context) *> IO.blocking {
      context.client.putObject(
        PutObjectArgs
          .builder()
          .bucket(context.config.bucket)
          .`object`(sourceKey)
          .stream(ByteArrayInputStream(bytes), bytes.length.toLong, -1)
          .contentType("text/plain; charset=utf-8")
          .build()
      )
      ()
    }

  /** 读取单个源码对象；对象不存在返回 None，其它存储错误继续暴露，内容非法视为存储损坏。 */
  def readSource(context: SubmissionProgramStorageContext, sourceKey: String): IO[Option[SubmissionSourceCode]] =
    ensureBucket(context) *> IO.blocking {
      try
        val inputStream = context.client.getObject(
          GetObjectArgs
            .builder()
            .bucket(context.config.bucket)
            .`object`(sourceKey)
            .build()
        )
        try
          Some(
            sourceCodeFromBytes(inputStream.readAllBytes())
              .fold(message => throw IllegalStateException(s"Invalid stored submission source: $message"), identity)
          )
        finally inputStream.close()
      catch
        case error: ErrorResponseException if MinioErrorHandling.isObjectNotFound(error) =>
          None
    }

  /** 删除单个源码对象；先读后删以返回对象是否存在。 */
  def deleteSource(context: SubmissionProgramStorageContext, sourceKey: String): IO[Boolean] =
    readSource(context, sourceKey).flatMap {
      case None => IO.pure(false)
      case Some(_) =>
        ensureBucket(context) *> IO.blocking {
          context.client.removeObject(
            RemoveObjectArgs
              .builder()
              .bucket(context.config.bucket)
              .`object`(sourceKey)
              .build()
          )
          true
        }
    }

  /** 读取 manifest 的默认源码；manifest 异常或对象缺失以 Left 返回。 */
  def readDefaultSource(
    context: SubmissionProgramStorageContext,
    manifest: SubmissionProgramManifest
  ): IO[Either[String, SubmissionSourceCode]] =
    manifest.defaultProgram match
      case Left(message) => IO.pure(Left(message))
      case Right(program) =>
        readSource(context, program.sourceKey).map {
          case Some(sourceCode) => Right(sourceCode)
          case None => Left(s"Submission source object was not found: ${program.sourceKey}.")
        }

  /** 读取 manifest 中所有角色源码；任一对象缺失会返回 Left。 */
  def readSources(
    context: SubmissionProgramStorageContext,
    manifest: SubmissionProgramManifest
  ): IO[Either[String, Map[String, SubmissionSourceCode]]] =
    manifest.programs.toList
      .traverse { case (role, program) =>
        readSource(context, program.sourceKey).map {
          case Some(sourceCode) => Right(role -> sourceCode)
          case None => Left(s"Submission source object was not found: ${program.sourceKey}.")
        }
      }
      .map(_.sequence.map(_.toMap))

  /** 删除 manifest 中所有源码对象；单个 deleteSource 的返回值会被忽略。 */
  def deleteManifest(context: SubmissionProgramStorageContext, manifest: SubmissionProgramManifest): IO[Unit] =
    manifest.programs.values.toList.traverse_(program => deleteSource(context, program.sourceKey).void)

  /** 将 UTF-8 字节解析为源码领域类型，并复用源码大小/非空校验。 */
  def sourceCodeFromBytes(bytes: Array[Byte]): Either[String, SubmissionSourceCode] =
    SubmissionSourceCode.parse(String(bytes, StandardCharsets.UTF_8))

  private def ensureBucket(context: SubmissionProgramStorageContext): IO[Unit] =
    IO.blocking {
      val exists = context.client.bucketExists(BucketExistsArgs.builder().bucket(context.config.bucket).build())
      if !exists then
        context.client.makeBucket(MakeBucketArgs.builder().bucket(context.config.bucket).build())
    }
