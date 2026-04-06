package domains.problem.application

import cats.effect.IO
import domains.problem.model.{ProblemDataFilename, ProblemSlug}

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.*
import java.util.zip.ZipInputStream

object ProblemDataStorage:

  private def rootDirectory: Path =
    Paths.get(sys.props.getOrElse("user.dir", "."), "problems")

  private def dataDirectory(problemSlug: ProblemSlug): Path =
    rootDirectory.resolve(problemSlug.value).resolve("data")

  def listFiles(problemSlug: ProblemSlug): IO[List[ProblemDataFilename]] =
    IO.blocking {
      val directory = dataDirectory(problemSlug)
      if !Files.exists(directory) then Nil
      else
        val stream = Files.list(directory)
        try
          stream.iterator().asScala
            .filter(path => Files.isRegularFile(path))
            .map(path => ProblemDataFilename.unsafe(path.getFileName.toString))
            .toList
            .sortBy(_.value)
        finally stream.close()
    }

  def writeFile(problemSlug: ProblemSlug, filename: ProblemDataFilename, bytes: Array[Byte]): IO[ProblemDataFilename] =
    IO.blocking {
      val sanitizedFilename = sanitizeFilename(filename)
      val directory = dataDirectory(problemSlug)
      Files.createDirectories(directory)
      if sanitizedFilename.value.toLowerCase.endsWith(".zip") then
        unzipIntoDirectory(directory, bytes)
      else
        Files.write(
          directory.resolve(sanitizedFilename.value),
          bytes,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
        )
      sanitizedFilename
    }

  def readFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Option[(ProblemDataFilename, Array[Byte])]] =
    IO.blocking {
      val sanitizedFilename = sanitizeFilename(filename)
      val path = dataDirectory(problemSlug).resolve(sanitizedFilename.value)
      if Files.exists(path) && Files.isRegularFile(path) then
        Some((sanitizedFilename, Files.readAllBytes(path)))
      else None
    }

  def deleteFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Boolean] =
    IO.blocking {
      val sanitizedFilename = sanitizeFilename(filename)
      val path = dataDirectory(problemSlug).resolve(sanitizedFilename.value)
      Files.deleteIfExists(path)
    }

  private def sanitizeFilename(filename: ProblemDataFilename): ProblemDataFilename =
    ProblemDataFilename.unsafe(Paths.get(filename.value.trim).getFileName.toString)

  private def unzipIntoDirectory(directory: Path, bytes: Array[Byte]): Unit =
    val zipInputStream = ZipInputStream(ByteArrayInputStream(bytes))
    try
      Iterator
        .continually(zipInputStream.getNextEntry)
        .takeWhile(_ != null)
        .foreach { entry =>
          if !entry.isDirectory then
            val sanitizedEntryName = sanitizeFilename(entry.getName)
            if sanitizedEntryName.value.nonEmpty then
              val targetPath = directory.resolve(sanitizedEntryName.value)
              Files.copy(zipInputStream, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
          zipInputStream.closeEntry()
        }
    finally zipInputStream.close()

  private def sanitizeFilename(filename: String): ProblemDataFilename =
    ProblemDataFilename.unsafe(Paths.get(filename.trim).getFileName.toString)
