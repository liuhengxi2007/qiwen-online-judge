package domains.problem.application

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSlug}

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

  @volatile private var current: ProblemDataStorage = LocalProblemDataStorage

  def install(storage: ProblemDataStorage): Unit =
    current = storage

  def listPaths(problemSlug: ProblemSlug): IO[List[ProblemDataPath]] =
    current.listPaths(problemSlug)

  def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest] =
    current.describeManifest(problemSlug)

  def writePath(problemSlug: ProblemSlug, path: ProblemDataPath, bytes: Array[Byte]): IO[ProblemDataPath] =
    current.writePath(problemSlug, path, bytes)

  def readPath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Option[(ProblemDataPath, Array[Byte])]] =
    current.readPath(problemSlug, path)

  def deletePath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean] =
    current.deletePath(problemSlug, path)

  def snapshotDirectory(problemSlug: ProblemSlug): IO[ProblemDataSnapshot] =
    current.snapshotDirectory(problemSlug)

  def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit] =
    current.deleteAllFiles(problemSlug)

  def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit] =
    current.restoreDirectory(problemSlug, snapshot)

  def listFiles(problemSlug: ProblemSlug): IO[List[ProblemDataFilename]] =
    listPaths(problemSlug).flatMap { paths =>
      IO.fromEither(
        paths
          .traverse(path => ProblemDataFilename.parse(path.fileName))
          .left
          .map(message => IllegalArgumentException(message))
      )
    }

  def writeFile(problemSlug: ProblemSlug, filename: ProblemDataFilename, bytes: Array[Byte]): IO[ProblemDataFilename] =
    writePath(problemSlug, ProblemDataPath.fromFilename(filename), bytes)
      .flatMap(path =>
        IO.fromEither(
          ProblemDataFilename
            .parse(path.fileName)
            .left
            .map(message => IllegalArgumentException(message))
        )
      )

  def readFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Option[(ProblemDataFilename, Array[Byte])]] =
    readPath(problemSlug, ProblemDataPath.fromFilename(filename)).flatMap {
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
    deletePath(problemSlug, ProblemDataPath.fromFilename(filename))
