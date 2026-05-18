package domains.problem.application

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.model.{ProblemDataFilename, ProblemDataManifest, ProblemDataPath, ProblemSlug}

trait ProblemDataStorage:
  import ProblemDataStorage.ProblemDataSnapshot

  def listPaths(problemSlug: ProblemSlug): IO[List[ProblemDataPath]]
  def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest]
  def snapshotDirectory(problemSlug: ProblemSlug): IO[ProblemDataSnapshot]
  def writePath(problemSlug: ProblemSlug, path: ProblemDataPath, bytes: Array[Byte]): IO[ProblemDataPath]
  def readPath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Option[(ProblemDataPath, Array[Byte])]]
  def deletePath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean]
  def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit]
  def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit]

object ProblemDataStorage:

  type ProblemDataSnapshot = Map[ProblemDataPath, Array[Byte]]

  extension (storage: ProblemDataStorage)

    def listFiles(problemSlug: ProblemSlug): IO[List[ProblemDataFilename]] =
      storage.listPaths(problemSlug).flatMap { paths =>
        IO.fromEither(
          paths
            .traverse(path => ProblemDataFilename.parse(path.fileName))
            .left
            .map(message => IllegalArgumentException(message))
        )
      }

    def writeFile(problemSlug: ProblemSlug, filename: ProblemDataFilename, bytes: Array[Byte]): IO[ProblemDataFilename] =
      storage.writePath(problemSlug, ProblemDataPath.fromFilename(filename), bytes)
        .flatMap(path =>
          IO.fromEither(
            ProblemDataFilename
              .parse(path.fileName)
              .left
              .map(message => IllegalArgumentException(message))
          )
        )

    def readFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Option[(ProblemDataFilename, Array[Byte])]] =
      storage.readPath(problemSlug, ProblemDataPath.fromFilename(filename)).flatMap {
        case None => IO.pure(None)
        case Some((path, bytes)) =>
          IO.fromEither(
            ProblemDataFilename
              .parse(path.fileName)
              .left
              .map(message => IllegalArgumentException(message))
          ).map(parsed => Some((parsed, bytes)))
      }

    def deleteFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Boolean] =
      storage.deletePath(problemSlug, ProblemDataPath.fromFilename(filename))
