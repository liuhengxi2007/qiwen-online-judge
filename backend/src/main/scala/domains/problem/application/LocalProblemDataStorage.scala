package domains.problem.application

import cats.effect.IO
import domains.problem.application.ProblemDataStorage.ProblemDataSnapshot
import domains.problem.model.{ProblemDataPath, ProblemSlug}
import domains.shared.upload.{FileUploadPolicy, FileUploadPreparation}

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.*

object LocalProblemDataStorage extends ProblemDataStorage:

  private def rootDirectory: Path =
    Paths.get(sys.props.getOrElse("user.dir", "."), "problems")

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
      val preparedFiles =
        if sanitizedPath.value.toLowerCase.endsWith(".zip") then
          FileUploadPreparation
            .prepareArchive(bytes, None, FileUploadPolicy.preserve)
            .fold(message => throw IllegalArgumentException(message), identity)
        else
          List(
            FileUploadPreparation
              .prepareFile(sanitizedPath.toStoredFilePath, bytes, FileUploadPolicy.preserve)
              .fold(message => throw IllegalArgumentException(message), identity)
          )

      preparedFiles.foreach { preparedFile =>
        val targetPath = resolveTargetPath(directory, ProblemDataPath(preparedFile.path.value))
        Files.createDirectories(targetPath.getParent)
        Files.write(
          targetPath,
          preparedFile.bytes,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
        )
      }
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
    IO.blocking {
      val directory = dataDirectory(problemSlug)
      val resolvedPath = resolveTargetPath(directory, sanitizePath(path))
      val deleted = Files.deleteIfExists(resolvedPath)
      if deleted then deleteEmptyAncestors(directory, resolvedPath.getParent)
      deleted
    }

  override def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit] =
    IO.blocking {
      val directory = dataDirectory(problemSlug)
      if Files.exists(directory) then clearDirectory(directory)
    }

  override def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit] =
    IO.blocking {
      val directory = dataDirectory(problemSlug)
      Files.createDirectories(directory)
      clearDirectory(directory)
      snapshot.foreach { case (path, bytes) =>
        val resolvedPath = resolveTargetPath(directory, path)
        Files.createDirectories(resolvedPath.getParent)
        Files.write(
          resolvedPath,
          bytes,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
        )
      }
    }

  private def clearDirectory(directory: Path): Unit =
    val stream = Files.walk(directory)
    try
      stream.iterator().asScala
        .toList
        .sortBy(_.getNameCount)(Ordering[Int].reverse)
        .filterNot(_ == directory)
        .foreach(path => Files.deleteIfExists(path))
    finally stream.close()

  private def deleteEmptyAncestors(rootDirectory: Path, startingDirectory: Path | Null): Unit =
    Option(startingDirectory).foreach { currentDirectory =>
      if currentDirectory != rootDirectory && Files.isDirectory(currentDirectory) then
        val childStream = Files.list(currentDirectory)
        try
          if !childStream.iterator().hasNext then
            Files.deleteIfExists(currentDirectory)
            deleteEmptyAncestors(rootDirectory, currentDirectory.getParent)
        finally childStream.close()
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
