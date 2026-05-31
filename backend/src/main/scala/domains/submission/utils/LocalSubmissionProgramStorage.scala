package domains.submission.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

class LocalSubmissionProgramStorage(rootDirectory: Path) extends SubmissionProgramStorage:

  def this() = this(Paths.get(sys.props.getOrElse("user.dir", "."), "submission-programs"))

  override def writeSource(sourceKey: String, sourceCode: SubmissionSourceCode): IO[Unit] =
    IO.blocking {
      val targetPath = resolveTargetPath(sourceKey)
      Files.createDirectories(targetPath.getParent)
      Files.write(
        targetPath,
        SubmissionProgramManifest.sourceBytes(sourceCode),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )
      ()
    }

  override def readSource(sourceKey: String): IO[Option[SubmissionSourceCode]] =
    IO.blocking {
      val targetPath = resolveTargetPath(sourceKey)
      if Files.exists(targetPath) && Files.isRegularFile(targetPath) then
        Some(
          SubmissionProgramStorage
            .sourceCodeFromBytes(Files.readAllBytes(targetPath))
            .fold(message => throw IllegalStateException(s"Invalid stored submission source: $message"), identity)
        )
      else None
    }

  override def deleteSource(sourceKey: String): IO[Boolean] =
    val normalizedRoot = rootDirectory.toAbsolutePath.normalize()
    IO.blocking {
      val targetPath = resolveTargetPath(sourceKey)
      val deleted = Files.deleteIfExists(targetPath)
      (deleted, targetPath.getParent)
    }.flatMap {
      case (true, parentDirectory) => deleteEmptyAncestors(normalizedRoot, parentDirectory).as(true)
      case (false, _) => IO.pure(false)
    }

  private def resolveTargetPath(sourceKey: String): Path =
    val normalizedRoot = rootDirectory.toAbsolutePath.normalize()
    val resolvedPath = normalizedRoot.resolve(sourceKey).normalize()
    if !resolvedPath.startsWith(normalizedRoot) then
      throw IllegalStateException(s"Submission source object key escaped storage root: $sourceKey")
    resolvedPath

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

object LocalSubmissionProgramStorage extends LocalSubmissionProgramStorage()
