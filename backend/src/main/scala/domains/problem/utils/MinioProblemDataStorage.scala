package domains.problem.utils



import cats.effect.IO
import domains.problem.utils.ProblemDataStorage.ProblemDataSnapshot
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import scala.jdk.CollectionConverters.*

/** 基于 MinIO 的题目数据存储实现；对象 key 按 problems/{slug}/data/{path} 命名。 */
final class MinioProblemDataStorage(config: MinioProblemDataStorageConfig) extends ProblemDataStorage:

  private val client =
    MinioClient
      .builder()
      .endpoint(config.endpoint)
      .credentials(config.accessKey, config.secretKey)
      .build()

  /** 扫描题目对象前缀并返回相对路径列表；会在需要时创建 bucket。 */
  override def listPaths(problemSlug: ProblemSlug): IO[List[ProblemDataPath]] =
    ensureBucket() *> IO.blocking {
      client
        .listObjects(
          io.minio.ListObjectsArgs
            .builder()
            .bucket(config.bucket)
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
  override def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest] =
    for
      snapshot <- snapshotDirectory(problemSlug)
      entries = snapshot.toList
        .sortBy(_._1.value)
        .map { case (path, bytes) =>
          ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))
        }
    yield ProblemDataManifest.fromEntries(problemSlug, entries)

  /** 读取当前题目目录完整快照，用于失败补偿。 */
  override def snapshotDirectory(problemSlug: ProblemSlug): IO[ProblemDataSnapshot] =
    listPaths(problemSlug).flatMap { paths =>
      paths.foldLeft(IO.pure(Map.empty[ProblemDataPath, Array[Byte]])) { (accIo, path) =>
        for
          acc <- accIo
          maybeContent <- readPath(problemSlug, path)
        yield maybeContent match
          case Some((storedPath, bytes)) => acc.updated(storedPath, bytes)
          case None => acc
      }
    }

  /** 上传或覆盖单个题目数据对象。 */
  override def writePath(problemSlug: ProblemSlug, path: ProblemDataPath, bytes: Array[Byte]): IO[ProblemDataPath] =
    ensureBucket() *> IO.blocking {
      client.putObject(
        PutObjectArgs
          .builder()
          .bucket(config.bucket)
          .`object`(toObjectKey(problemSlug, path))
          .stream(ByteArrayInputStream(bytes), bytes.length.toLong, -1)
          .contentType("application/octet-stream")
          .build()
      )
      path
    }

  /** 读取单个题目数据对象；MinIO 缺失对象被映射为 None。 */
  override def readPath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Option[(ProblemDataPath, Array[Byte])]] =
    ensureBucket() *> IO.blocking {
      try
        val inputStream = client.getObject(
          GetObjectArgs
            .builder()
            .bucket(config.bucket)
            .`object`(toObjectKey(problemSlug, path))
            .build()
        )
        try Some((path, inputStream.readAllBytes()))
        finally inputStream.close()
      catch
        // FIXME-CN: 这里把所有 MinIO ErrorResponseException 都当成对象不存在；权限、桶状态或服务端错误会被静默映射为 None。
        case _: io.minio.errors.ErrorResponseException =>
          None
    }

  /** 删除单个对象；先读后删以返回是否存在。 */
  override def deletePath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean] =
    readPath(problemSlug, path).flatMap {
      case None => IO.pure(false)
      case Some(_) =>
        ensureBucket() *> IO.blocking {
          client.removeObject(
            RemoveObjectArgs
              .builder()
              .bucket(config.bucket)
              .`object`(toObjectKey(problemSlug, path))
              .build()
          )
          true
        }
    }

  /** 删除题目前缀下所有数据对象。 */
  override def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit] =
    listPaths(problemSlug).flatMap(paths => paths.foldLeft(IO.unit)((accIo, path) => accIo *> deletePath(problemSlug, path).void))

  /** 以快照替换题目目录内容；先清空再逐个写回。 */
  override def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit] =
    // FIXME-CN: MinIO 恢复不是事务操作，清空后逐个写回期间失败会留下部分恢复状态，调用方补偿边界需要单独评估。
    deleteAllFiles(problemSlug) *> snapshot.toList.foldLeft(IO.unit) { case (accIo, (path, bytes)) =>
      accIo *> writePath(problemSlug, path, bytes).void
    }

  private def ensureBucket(): IO[Unit] =
    IO.blocking {
      val exists = client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())
      if !exists then
        client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
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
