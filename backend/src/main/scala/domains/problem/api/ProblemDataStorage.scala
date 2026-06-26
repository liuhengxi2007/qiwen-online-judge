package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import io.minio.errors.ErrorResponseException
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}
import shared.application.upload.MinioErrorHandling

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import scala.jdk.CollectionConverters.*

/** 题目数据对象存储上下文；路径均为题目 slug 下的相对路径。 */
final case class ProblemDataStorageContext(
  config: MinioProblemDataStorageConfig,
  client: MinioClient
)

/** 题目数据存储函数集合；对象 key 按 problems/{slug}/data/{path} 命名。 */
object ProblemDataStorage:

  type ProblemDataSnapshot = Map[ProblemDataPath, Array[Byte]]

  /** 创建 MinIO-backed 存储上下文；client 在进程生命周期内复用。 */
  def create(config: MinioProblemDataStorageConfig): ProblemDataStorageContext =
    ProblemDataStorageContext(
      config = config,
      client = MinioClient
        .builder()
        .endpoint(config.endpoint)
        .credentials(config.accessKey, config.secretKey)
        .build()
    )

  /** 扫描题目对象前缀并返回相对路径列表；会在需要时创建 bucket。 */
  def listPaths(context: ProblemDataStorageContext, problemSlug: ProblemSlug): IO[List[ProblemDataPath]] =
    ensureBucket(context) *> IO.blocking {
      context.client
        .listObjects(
          io.minio.ListObjectsArgs
            .builder()
            .bucket(context.config.bucket)
            .prefix(prefixFor(problemSlug))
            .recursive(true)
            .build()
        )
        .iterator()
        .asScala
        .toList
        .map(_.get())
        .filterNot(_.isDir)
        .map(item => parseStoredPath(fromObjectKey(problemSlug, item.objectName())))
        .sortBy(_.value)
    }

  /** 读取题目所有文件内容并生成清单版本；成本与数据集大小成正比。 */
  def describeManifest(context: ProblemDataStorageContext, problemSlug: ProblemSlug): IO[ProblemDataManifest] =
    for
      snapshot <- snapshotDirectory(context, problemSlug)
      entries = snapshot.toList
        .sortBy(_._1.value)
        .map { case (path, bytes) =>
          ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))
        }
    yield ProblemDataManifest.fromEntries(problemSlug, entries)

  /** 读取当前题目目录完整快照，用于失败补偿。 */
  def snapshotDirectory(context: ProblemDataStorageContext, problemSlug: ProblemSlug): IO[ProblemDataSnapshot] =
    listPaths(context, problemSlug).flatMap { paths =>
      paths.foldLeft(IO.pure(Map.empty[ProblemDataPath, Array[Byte]])) { (accIo, path) =>
        for
          acc <- accIo
          maybeContent <- readPath(context, problemSlug, path)
        yield maybeContent match
          case Some((storedPath, bytes)) => acc.updated(storedPath, bytes)
          case None => acc
      }
    }

  /** 上传或覆盖单个题目数据对象。 */
  def writePath(
    context: ProblemDataStorageContext,
    problemSlug: ProblemSlug,
    path: ProblemDataPath,
    bytes: Array[Byte]
  ): IO[ProblemDataPath] =
    ensureBucket(context) *> IO.blocking {
      context.client.putObject(
        PutObjectArgs
          .builder()
          .bucket(context.config.bucket)
          .`object`(toObjectKey(problemSlug, path))
          .stream(ByteArrayInputStream(bytes), bytes.length.toLong, -1)
          .contentType("application/octet-stream")
          .build()
      )
      path
    }

  /** 读取单个题目数据对象；MinIO 对象缺失被映射为 None，其它存储错误继续暴露。 */
  def readPath(
    context: ProblemDataStorageContext,
    problemSlug: ProblemSlug,
    path: ProblemDataPath
  ): IO[Option[(ProblemDataPath, Array[Byte])]] =
    ensureBucket(context) *> IO.blocking {
      try
        val inputStream = context.client.getObject(
          GetObjectArgs
            .builder()
            .bucket(context.config.bucket)
            .`object`(toObjectKey(problemSlug, path))
            .build()
        )
        try Some((path, inputStream.readAllBytes()))
        finally inputStream.close()
      catch
        case error: ErrorResponseException if MinioErrorHandling.isObjectNotFound(error) =>
          None
    }

  /** 删除单个对象；先读后删以返回是否存在。 */
  def deletePath(context: ProblemDataStorageContext, problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean] =
    readPath(context, problemSlug, path).flatMap {
      case None => IO.pure(false)
      case Some(_) =>
        ensureBucket(context) *> IO.blocking {
          context.client.removeObject(
            RemoveObjectArgs
              .builder()
              .bucket(context.config.bucket)
              .`object`(toObjectKey(problemSlug, path))
              .build()
          )
          true
        }
    }

  /** 删除题目前缀下所有数据对象。 */
  def deleteAllFiles(context: ProblemDataStorageContext, problemSlug: ProblemSlug): IO[Unit] =
    listPaths(context, problemSlug).flatMap(paths => paths.foldLeft(IO.unit)((accIo, path) => accIo *> deletePath(context, problemSlug, path).void))

  /** 以快照替换题目目录内容；先恢复快照对象，再删除快照外新增对象。 */
  def restoreDirectory(
    context: ProblemDataStorageContext,
    problemSlug: ProblemSlug,
    snapshot: ProblemDataSnapshot
  ): IO[Unit] =
    val snapshotPaths = snapshot.keySet
    snapshot.toList.foldLeft(IO.unit) { case (accIo, (path, bytes)) =>
      accIo *> writePath(context, problemSlug, path, bytes).void
    } *> listPaths(context, problemSlug).flatMap { currentPaths =>
      currentPaths.filterNot(snapshotPaths.contains).foldLeft(IO.unit) { (accIo, path) =>
        accIo *> deletePath(context, problemSlug, path).void
      }
    }

  /** 将路径列表转换为根文件名列表；遇到无法表示为文件名的路径会失败。 */
  def listFiles(context: ProblemDataStorageContext, problemSlug: ProblemSlug): IO[List[ProblemDataFilename]] =
    listPaths(context, problemSlug).flatMap { paths =>
      IO.fromEither(
        paths
          .traverse(path => ProblemDataFilename.parse(path.fileName))
          .left
          .map(message => IllegalArgumentException(message))
      )
    }

  /** 以根文件名写入对象，用于旧接口兼容。 */
  def writeFile(
    context: ProblemDataStorageContext,
    problemSlug: ProblemSlug,
    filename: ProblemDataFilename,
    bytes: Array[Byte]
  ): IO[ProblemDataFilename] =
    writePath(context, problemSlug, ProblemDataPath.fromFilename(filename), bytes)
      .flatMap(path =>
        IO.fromEither(
          ProblemDataFilename
            .parse(path.fileName)
            .left
            .map(message => IllegalArgumentException(message))
        )
      )

  /** 以根文件名读取对象，用于旧接口兼容。 */
  def readFile(
    context: ProblemDataStorageContext,
    problemSlug: ProblemSlug,
    filename: ProblemDataFilename
  ): IO[Option[(ProblemDataFilename, Array[Byte])]] =
    readPath(context, problemSlug, ProblemDataPath.fromFilename(filename)).flatMap {
      case None => IO.pure(None)
      case Some((path, bytes)) =>
        IO.fromEither(
          ProblemDataFilename
            .parse(path.fileName)
            .left
            .map(message => IllegalArgumentException(message))
        ).map(parsed => Some((parsed, bytes)))
    }

  /** 以根文件名删除对象，用于旧接口兼容。 */
  def deleteFile(context: ProblemDataStorageContext, problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Boolean] =
    deletePath(context, problemSlug, ProblemDataPath.fromFilename(filename))

  private def ensureBucket(context: ProblemDataStorageContext): IO[Unit] =
    IO.blocking {
      val exists = context.client.bucketExists(BucketExistsArgs.builder().bucket(context.config.bucket).build())
      if !exists then
        context.client.makeBucket(MakeBucketArgs.builder().bucket(context.config.bucket).build())
    }

  private def prefixFor(problemSlug: ProblemSlug): String =
    s"problems/${problemSlug.value}/data/"

  private def toObjectKey(problemSlug: ProblemSlug, path: ProblemDataPath): String =
    s"${prefixFor(problemSlug)}${path.value}"

  private def fromObjectKey(problemSlug: ProblemSlug, objectKey: String): String =
    objectKey.stripPrefix(prefixFor(problemSlug))

  private def parseStoredPath(rawPath: String): ProblemDataPath =
    ProblemDataPath.parse(rawPath).fold(message => throw IllegalStateException(message), identity)

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
