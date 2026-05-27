package domains.problem.application



import cats.effect.IO
import domains.problem.application.ProblemDataStorage.ProblemDataSnapshot
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.security.MessageDigest
import scala.jdk.CollectionConverters.*

class LocalProblemDataStorage(rootDirectory: Path) extends ProblemDataStorage:

  def this() = this(Paths.get(sys.props.getOrElse("user.dir", "."), "problems"))

  private def dataDirectory(problemSlug: ProblemSlug): Path =
    rootDirectory.resolve(problemSlug.value).resolve("data")

  override def listPaths(problemSlug: ProblemSlug): IO[List[ProblemDataPath]] =
    IO.blocking {
      val directory = dataDirectory(problemSlug)
      if !Files.exists(directory) then Nil
      else
        val stream = Files.walk(directory)
        try
          stream.iterator().asScala
            .filter(path => Files.isRegularFile(path))
            .map(path => toProblemDataPath(directory, path))
            .toList
            .sortBy(_.value)
        finally stream.close()
    }

  override def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest] =
    snapshotDirectory(problemSlug).map { snapshot =>
      val entries = snapshot.toList
        .sortBy(_._1.value)
        .map { case (path, bytes) =>
          ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))
        }
      ProblemDataManifest.fromEntries(problemSlug, entries)
    }

  override def snapshotDirectory(problemSlug: ProblemSlug): IO[ProblemDataSnapshot] =
    IO.blocking {
      val directory = dataDirectory(problemSlug)
      if !Files.exists(directory) then Map.empty
      else
        val stream = Files.walk(directory)
        try
          stream.iterator().asScala
            .filter(path => Files.isRegularFile(path))
            .map { path =>
              val relativePath = toProblemDataPath(directory, path)
              relativePath -> Files.readAllBytes(path)
            }
            .toMap
        finally stream.close()
    }

  override def writePath(problemSlug: ProblemSlug, path: ProblemDataPath, bytes: Array[Byte]): IO[ProblemDataPath] =
    IO.blocking {
      val sanitizedPath = sanitizePath(path)
      val directory = dataDirectory(problemSlug)
      Files.createDirectories(directory)
      val targetPath = resolveTargetPath(directory, sanitizedPath)
      Files.createDirectories(targetPath.getParent)
      Files.write(
        targetPath,
        bytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )
      sanitizedPath
    }

  override def readPath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Option[(ProblemDataPath, Array[Byte])]] =
    IO.blocking {
      val sanitizedPath = sanitizePath(path)
      val resolvedPath = resolveTargetPath(dataDirectory(problemSlug), sanitizedPath)
      if Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath) then
        Some((sanitizedPath, Files.readAllBytes(resolvedPath)))
      else None
    }

  override def deletePath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean] =
    val directory = dataDirectory(problemSlug)
    IO.blocking {
      val resolvedPath = resolveTargetPath(directory, sanitizePath(path))
      val deleted = Files.deleteIfExists(resolvedPath)
      (deleted, resolvedPath.getParent)
    }.flatMap {
      case (true, parentDirectory) => deleteEmptyAncestors(directory, parentDirectory).as(true)
      case (false, _) => IO.pure(false)
    }

  override def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit] =
    val directory = dataDirectory(problemSlug)
    IO.blocking(Files.exists(directory)).flatMap {
      case true => clearDirectory(directory)
      case false => IO.unit
    }

  override def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit] =
    val directory = dataDirectory(problemSlug)
    IO.blocking(Files.createDirectories(directory)) *>
      clearDirectory(directory) *>
      snapshot.toList.foldLeft(IO.unit) { case (acc, (path, bytes)) =>
        acc *> IO.blocking {
          val resolvedPath = resolveTargetPath(directory, path)
          Files.createDirectories(resolvedPath.getParent)
          Files.write(
            resolvedPath,
            bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
          )
          ()
        }
      }

  private def clearDirectory(directory: Path): IO[Unit] =
    IO.blocking {
      val stream = Files.walk(directory)
      try
        stream.iterator().asScala
          .toList
          .sortBy(_.getNameCount)(Ordering[Int].reverse)
          .filterNot(_ == directory)
          .foreach(path => Files.deleteIfExists(path))
      finally stream.close()
    }

  private def deleteEmptyAncestors(rootDirectory: Path, startingDirectory: Path | Null): IO[Unit] =
    Option(startingDirectory) match
      case None => IO.unit
      case Some(currentDirectory) =>
        IO.blocking {
          if currentDirectory != rootDirectory && Files.isDirectory(currentDirectory) then
            val childStream = Files.list(currentDirectory)
            try
              if !childStream.iterator().hasNext then
                Files.deleteIfExists(currentDirectory)
                Some(currentDirectory.getParent)
              else None
            finally childStream.close()
          else None
        }.flatMap {
          case Some(parentDirectory) => deleteEmptyAncestors(rootDirectory, parentDirectory)
          case None => IO.unit
        }

  private def toProblemDataPath(rootDirectory: Path, path: Path): ProblemDataPath =
    val relativePath = rootDirectory.relativize(path).toString.replace('\\', '/')
    parsePath(relativePath, s"problem data path ${path.toString}")

  private def resolveTargetPath(rootDirectory: Path, path: ProblemDataPath): Path =
    val normalizedRoot = rootDirectory.toAbsolutePath.normalize()
    val resolvedPath = normalizedRoot.resolve(path.value).normalize()
    if !resolvedPath.startsWith(normalizedRoot) then
      throw IllegalStateException(s"Problem data path escaped storage root: ${path.value}")
    resolvedPath

  private def sanitizePath(path: ProblemDataPath): ProblemDataPath =
    parsePath(path.value, s"problem data path ${path.value}")

  private def parsePath(rawPath: String, label: String): ProblemDataPath =
    ProblemDataPath
      .parse(rawPath)
      .fold(message => throw IllegalStateException(s"Invalid $label: $message"), identity)

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

object LocalProblemDataStorage extends LocalProblemDataStorage()
