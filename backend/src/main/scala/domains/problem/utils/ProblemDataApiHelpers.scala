package domains.problem.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.response.{ProblemDataTreeResponse, ProblemDetail}
import domains.problem.objects.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.objects.response.{ProblemDataTreeNode, ProblemDataTreeNodeKind}
import domains.problem.table.problem.ProblemQueryTable

import java.security.MessageDigest
import java.sql.Connection

/** 题目数据 API 的共享辅助；封装行锁、刷新、清单转换和目录树构造。 */
object ProblemDataApiHelpers:

  /** 按 slug 加载题目并加行锁；用于数据写入、删除和 ready 状态切换。 */
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

  /** 重新读取题目详情并标记可管理；题目消失时返回调用方提供的内部错误信息。 */
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

  /** 将预处理后的文件逐个转换为题目路径并写入对象存储。 */
  def writePreparedFiles(
    problemDataStorage: ProblemDataStorageContext,
    problemSlug: ProblemSlug,
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): IO[Unit] =
    preparedFiles.traverse_ { preparedFile =>
      IO.fromEither(
        ProblemDataUploadPreparation
          .toProblemDataPath(preparedFile.path)
          .leftMap(message => IllegalArgumentException(message))
      ).flatMap(path => ProblemDataStorage.writePath(problemDataStorage, problemSlug, path, preparedFile.bytes).void)
    }

  /** 选择本次上传中排序最后一个文件名作为题目数据摘要文件名；空上传会失败。 */
  def summaryFilenameFor(
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): Either[String, ProblemDataFilename] =
    preparedFiles
      .sortBy(_.path.value)
      .lastOption
      .toRight("Uploaded archive does not contain any files.")
      .flatMap(file => ProblemDataFilename.parse(file.path.fileName))

  /** 将预处理文件转换为数据库清单条目；包含大小和 sha256。 */
  def toManifestEntries(
    preparedFiles: List[shared.application.upload.PreparedUploadFile]
  ): List[ProblemDataManifestEntry] =
    preparedFiles.flatMap { preparedFile =>
      ProblemDataUploadPreparation.toProblemDataPath(preparedFile.path).toOption.map { path =>
        ProblemDataManifestEntry(path = path, sizeBytes = preparedFile.bytes.length.toLong, sha256 = sha256Hex(preparedFile.bytes))
      }
    }

  /** 从现有清单中选择摘要文件名；没有文件或文件名非法时返回 None。 */
  def summaryFilenameForEntries(entries: List[ProblemDataManifestEntry]): Option[ProblemDataFilename] =
    entries
      .sortBy(_.path.value)
      .lastOption
      .flatMap(entry => ProblemDataFilename.parse(entry.path.fileName).toOption)

  /** 根据清单路径推导目录节点并合并文件节点，输出前端使用的数据树。 */
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
