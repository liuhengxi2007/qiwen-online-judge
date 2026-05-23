package domains.problem.application



import cats.effect.IO
import domains.problem.application.ProblemDataStorage.ProblemDataSnapshot
import domains.problem.model.{ProblemDataManifest, ProblemDataManifestEntry, ProblemDataPath, ProblemSlug}
import io.minio.{BucketExistsArgs, GetObjectArgs, MakeBucketArgs, MinioClient, PutObjectArgs, RemoveObjectArgs}
import io.minio.messages.Item

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import scala.jdk.CollectionConverters.*

final class MinioProblemDataStorage(config: MinioProblemDataStorageConfig) extends ProblemDataStorage:

  private val client =
    MinioClient
      .builder()
      .endpoint(config.endpoint)
      .credentials(config.accessKey, config.secretKey)
      .build()

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

  override def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest] =
    for
      snapshot <- snapshotDirectory(problemSlug)
      entries = snapshot.toList
        .sortBy(_._1.value)
        .map { case (path, bytes) =>
          ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))
        }
    yield ProblemDataManifest.fromEntries(problemSlug, entries)

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
        case _: io.minio.errors.ErrorResponseException =>
          None
    }

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

  override def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit] =
    listPaths(problemSlug).flatMap(paths => paths.foldLeft(IO.unit)((accIo, path) => accIo *> deletePath(problemSlug, path).void))

  override def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit] =
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
