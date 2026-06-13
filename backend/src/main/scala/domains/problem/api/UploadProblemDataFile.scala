package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.{ProblemDataStorage, ProblemDataStorageContext, ProblemDataUploadPreparation}
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.response.ProblemDataUploadResult
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.nio.charset.StandardCharsets
import java.time.Instant

/** 上传题目数据单文件的管理端 API；路径可由表单字段指定，也可退回到上传文件名。 */
final case class UploadProblemDataFile(problemDataStorage: ProblemDataStorageContext)
    extends AuthenticatedApi[(ProblemManagementContext, ProblemDataPath, Array[Byte]), ProblemDataUploadResult]:

  private val maxFileBytes = 256 * 1024 * 1024
  private val maxTextPartBytes = 4096

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataUploadResult] = summon[Encoder[ProblemDataUploadResult]]

  /** 解析 multipart 文件、题目上下文和目标相对路径；路径非法或缺失时拒绝请求。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemManagementContext, ProblemDataPath, Array[Byte])] =
    for
      context <- ProblemManagementContext.decode(request, pathParams)
      multipart <- request.as[Multipart[IO]]
      file <- extractNamedBinaryPart(multipart, "file").flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart file field 'file' is required."))
      }
      (filePart, bytes) = file
      maybePath <- extractOptionalPathField(multipart, "path")
      resolvedPath <- maybePath.orElse(filePart.filename.flatMap(name => ProblemDataPath.parse(name).toOption)) match
        case Some(path) => IO.pure(path)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart upload requires a valid 'path' field or uploaded filename."))
    yield (context, resolvedPath, bytes)

  /** 校验单文件上传策略和管理权限后写入数据文件。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemManagementContext, ProblemDataPath, Array[Byte])
  ): IO[ProblemDataUploadResult] =
    val (context, path, bytes) = input
    ProblemDataUploadPreparation.prepareSingleFile(path, bytes) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(preparedFile) =>
        ProblemDataApiHelpers.summaryFilenameFor(List(preparedFile)) match
          case Left(message) =>
            HttpApiError.raise(HttpApiError.badRequest(message))
          case Right(summaryFilename) =>
            ProblemManagementContext
              .requireManagedProblem(connection, actor, context)
              .flatMap(_ => uploadManagedProblemDataFile(connection, context.problemSlug, path, bytes))

  /** 对已授权题目写入单个数据文件；同步更新数据摘要和清单，失败时恢复对象存储快照。 */
  def uploadManagedProblemDataFile(
    connection: Connection,
    problemSlug: ProblemSlug,
    path: ProblemDataPath,
    bytes: Array[Byte]
  ): IO[ProblemDataUploadResult] =
    ProblemDataUploadPreparation.prepareSingleFile(path, bytes) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(preparedFile) =>
        ProblemDataApiHelpers.summaryFilenameFor(List(preparedFile)) match
          case Left(message) =>
            HttpApiError.raise(HttpApiError.badRequest(message))
          case Right(summaryFilename) =>
            ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
              for
                snapshot <- ProblemDataStorage.snapshotDirectory(problemDataStorage, problem.slug)
                uploadResult <- ProblemDataApiHelpers
                  .writePreparedFiles(problemDataStorage, problem.slug, List(preparedFile))
                  .flatMap(_ => ProblemDataStateTable.updateData(connection, problem.id, Instant.now(), summaryFilename))
                  .flatMap(_ =>
                    ProblemDataFileTable.upsertForProblem(
                      connection,
                      problem.id,
                      ProblemDataApiHelpers.toManifestEntries(List(preparedFile)),
                      Instant.now()
                    )
                  )
                  .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after data update."))
                  .map(problem => ProblemDataUploadResult(problem, uploadedFileCount = 1))
                  .handleErrorWith { error =>
                    ProblemDataStorage.restoreDirectory(problemDataStorage, problem.slug, snapshot) *> IO.raiseError(error)
                  }
              yield uploadResult
            }

  private def extractNamedBinaryPart(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[(Part[IO], Array[Byte])]] =
    multipart.parts.find(_.name.contains(fieldName)) match
      case None => IO.pure(None)
      case Some(part) => readPartBytes(part, maxFileBytes, "Problem data file").map(bytes => Some((part, bytes)))

  private def extractOptionalPathField(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[ProblemDataPath]] =
    multipart.parts.find(_.name.contains(fieldName)) match
      case None => IO.pure(None)
      case Some(part) =>
        decodeTextPart(part).flatMap { rawValue =>
          val normalized = rawValue.trim
          if normalized.isEmpty then IO.pure(None)
          else HttpApiError.fromEitherBadRequest(ProblemDataPath.parse(normalized).map(Some(_)))
        }

  private def decodeTextPart(part: Part[IO]): IO[String] =
    readPartBytes(part, maxTextPartBytes, "Multipart path field").map(bytes => String(bytes, StandardCharsets.UTF_8))

  private def readPartBytes(part: Part[IO], maxBytes: Int, label: String): IO[Array[Byte]] =
    part.body.take(maxBytes.toLong + 1L).compile.to(Array).flatMap { bytes =>
      if bytes.length > maxBytes then HttpApiError.raise(HttpApiError.badRequest(s"$label must be at most ${maxBytes / 1024 / 1024} MB."))
      else IO.pure(bytes)
    }
