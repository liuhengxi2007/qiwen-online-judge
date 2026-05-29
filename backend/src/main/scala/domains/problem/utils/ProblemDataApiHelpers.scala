package domains.problem.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.response.{ProblemDataTreeResponse, ProblemDetail}
import domains.problem.objects.{ProblemDataFilename, ProblemDataPath, ProblemDataTreeNode, ProblemDataTreeNodeKind, ProblemSlug}
import domains.problem.table.problem.ProblemQueryTable

import java.security.MessageDigest
import java.sql.Connection

object ProblemDataApiHelpers:

  def withProblemForUpdate[A](
    connection: Connection,
    problemSlug: ProblemSlug
  )(useProblem: ProblemDetail => IO[A]): IO[A] =
    ProblemQueryTable.findBySlugForUpdate(connection, problemSlug).flatMap {
      case None =>
        shared.api.HttpApiError.raise(shared.api.HttpApiError.notFound(shared.api.ApiMessages.problemNotFound))
      case Some(problem) =>
        useProblem(problem)
    }

  def refreshedManagedProblem(
    connection: Connection,
    problem: ProblemDetail,
    missingMessage: String
  ): IO[ProblemDetail] =
    ProblemQueryTable
      .findBySlug(connection, problem.slug)
      .flatMap {
        case Some(problem) => IO.pure(problem.copy(canManage = true))
        case None => shared.api.HttpApiError.raise(shared.api.HttpApiError.internal(missingMessage))
      }

  def writePreparedFiles(
    problemDataStorage: ProblemDataStorage,
    problemSlug: ProblemSlug,
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): IO[Unit] =
    preparedFiles.traverse_ { preparedFile =>
      IO.fromEither(
        ProblemDataUploadPreparation
          .toProblemDataPath(preparedFile.path)
          .leftMap(message => IllegalArgumentException(message))
      ).flatMap(path => problemDataStorage.writePath(problemSlug, path, preparedFile.bytes).void)
    }

  def summaryFilenameFor(
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): Either[String, ProblemDataFilename] =
    preparedFiles
      .sortBy(_.path.value)
      .lastOption
      .toRight("Uploaded archive does not contain any files.")
      .flatMap(file => ProblemDataFilename.parse(file.path.fileName))

  def toManifestEntries(
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): List[ProblemDataManifestEntry] =
    preparedFiles.flatMap { preparedFile =>
      ProblemDataUploadPreparation.toProblemDataPath(preparedFile.path).toOption.map { path =>
        ProblemDataManifestEntry(path = path, sizeBytes = preparedFile.bytes.length.toLong, sha256 = sha256Hex(preparedFile.bytes))
      }
    }

  def summaryFilenameForEntries(entries: List[ProblemDataManifestEntry]): Option[ProblemDataFilename] =
    entries
      .sortBy(_.path.value)
      .lastOption
      .flatMap(entry => ProblemDataFilename.parse(entry.path.fileName).toOption)

  def buildTreeResponse(entries: List[ProblemDataManifestEntry]): ProblemDataTreeResponse =
    val directoryNodes =
      entries
        .flatMap(entry => directoryPrefixes(entry.path))
        .distinct
        .sortBy(_.value)
        .map(path => ProblemDataTreeNode(path = path, kind = ProblemDataTreeNodeKind.Directory, sizeBytes = None))

    val fileNodes =
      entries
        .sortBy(_.path.value)
        .map(entry => ProblemDataTreeNode(path = entry.path, kind = ProblemDataTreeNodeKind.File, sizeBytes = Some(entry.sizeBytes)))

    ProblemDataTreeResponse(items = directoryNodes ++ fileNodes)

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  private def directoryPrefixes(path: ProblemDataPath): List[ProblemDataPath] =
    val segments = path.value.split('/').toList.dropRight(1)
    segments.indices.map(index => ProblemDataPath(segments.take(index + 1).mkString("/"))).toList
