package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.{ProblemDataStorage, ProblemDataUploadPreparation}
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.response.ProblemDataUploadResult
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import fs2.text
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

/** 上传题目数据 zip 归档的管理端 API；解包后写入对象存储、更新数据状态和文件清单。 */
final case class UploadProblemDataArchive(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemManagementContext, Option[ProblemDataPath], Array[Byte]), ProblemDataUploadResult]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/archive-imports")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataUploadResult] = summon[Encoder[ProblemDataUploadResult]]

  /** 解析 multipart 中的 zip 文件和可选目标目录；只接受 .zip 文件名。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemManagementContext, Option[ProblemDataPath], Array[Byte])] =
    for
      context <- ProblemManagementContext.decode(request, pathParams)
      multipart <- request.as[Multipart[IO]]
      file <- extractNamedBinaryPart(multipart, "file").flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart file field 'file' is required."))
      }
      (filePart, bytes) = file
      _ <- HttpApiError.ensure(
        filePart.filename.exists(_.toLowerCase.endsWith(".zip")),
        HttpApiError.badRequest("Multipart archive upload requires a .zip file.")
      )
      targetDirectory <- extractOptionalPathField(multipart, "targetDir")
    yield (context, targetDirectory, bytes)

  /** 校验压缩包结构与管理权限后上传；输出更新后的题目和写入文件数。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemManagementContext, Option[ProblemDataPath], Array[Byte])
  ): IO[ProblemDataUploadResult] =
    val (context, targetDirectory, bytes) = input
    ProblemDataUploadPreparation.prepareArchive(bytes, targetDirectory) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(preparedFiles) =>
        ProblemDataApiHelpers.summaryFilenameFor(preparedFiles) match
          case Left(message) =>
            HttpApiError.raise(HttpApiError.badRequest(message))
          case Right(summaryFilename) =>
            ProblemManagementContext
              .requireManagedProblem(connection, actor, context)
              .flatMap(_ => uploadManagedProblemDataArchive(connection, context.problemSlug, targetDirectory, bytes))

  /** 对已授权题目执行归档导入；失败时根据导入前对象存储快照恢复。 */
  def uploadManagedProblemDataArchive(
    connection: Connection,
    problemSlug: ProblemSlug,
    targetDirectory: Option[ProblemDataPath],
    bytes: Array[Byte]
  ): IO[ProblemDataUploadResult] =
    ProblemDataUploadPreparation.prepareArchive(bytes, targetDirectory) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(preparedFiles) =>
        ProblemDataApiHelpers.summaryFilenameFor(preparedFiles) match
          case Left(message) =>
            HttpApiError.raise(HttpApiError.badRequest(message))
          case Right(summaryFilename) =>
            ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
              for
                snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
                uploadResult <- ProblemDataApiHelpers
                  .writePreparedFiles(problemDataStorage, problem.slug, preparedFiles)
                  .flatMap(_ => ProblemDataStateTable.updateData(connection, problem.id, Instant.now(), summaryFilename))
                  .flatMap(_ =>
                    ProblemDataFileTable.upsertForProblem(
                      connection,
                      problem.id,
                      ProblemDataApiHelpers.toManifestEntries(preparedFiles),
                      Instant.now()
                    )
                  )
                  .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after data update."))
                  .map(problem => ProblemDataUploadResult(problem, preparedFiles.length))
                  .handleErrorWith { error =>
                    problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                  }
              yield uploadResult
            }

  private def extractNamedBinaryPart(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[(Part[IO], Array[Byte])]] =
    multipart.parts.find(_.name.contains(fieldName)) match
      case None => IO.pure(None)
      // FIXME-CN: 上传归档会在进入 prepareArchive 前完整读入内存，当前入口没有显式大小限制。
      case Some(part) => part.body.compile.to(Array).map(bytes => Some((part, bytes)))

  private def extractOptionalPathField(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[ProblemDataPath]] =
    multipart.parts.find(_.name.contains(fieldName)) match
      case None => IO.pure(None)
      case Some(part) =>
        // FIXME-CN: targetDir 文本 part 也会完整读入字符串且没有显式大小限制，恶意 multipart 可绕过归档 part 的资源预期。
        decodeTextPart(part).flatMap { rawValue =>
          val normalized = rawValue.trim
          if normalized.isEmpty then IO.pure(None)
          else HttpApiError.fromEitherBadRequest(ProblemDataPath.parse(normalized).map(Some(_)))
        }

  private def decodeTextPart(part: Part[IO]): IO[String] =
    part.body.through(text.utf8.decode).compile.string
