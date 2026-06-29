package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem_data_file.ProblemDataFileTable
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.util.zip.{ZipEntry, ZipOutputStream}

/** 下载题目数据压缩包的归档生成辅助。 */
object DownloadProblemDataArchive:

  type ReadPath = (ProblemSlug, ProblemDataPath) => IO[Option[(ProblemDataPath, Array[Byte])]]

  private val maxArchiveSourceBytes = 256L * 1024L * 1024L

  /** 按清单条目读取对象存储并组装 zip；调用方需保证 entries 来自同一题目。 */
  def archiveResponse(
    problemSlug: ProblemSlug,
    entries: List[ProblemDataManifestEntry],
    readPath: ReadPath
  ): IO[Response[IO]] =
    HttpApiError.ensure(
      entries.map(_.sizeBytes).sum <= maxArchiveSourceBytes,
      HttpApiError.badRequest(s"Problem data archive source files must total at most ${maxArchiveSourceBytes / 1024L / 1024L} MB.")
    ) *> archiveBytes(problemSlug, entries, readPath).map(bytes => binaryResponse(problemSlug, bytes))

  private def archiveBytes(
    problemSlug: ProblemSlug,
    entries: List[ProblemDataManifestEntry],
    readPath: ReadPath
  ): IO[Array[Byte]] =
    entries
      .sortBy(_.path.value)
      .traverse { entry =>
        readPath(problemSlug, entry.path).flatMap {
          case Some((_, bytes)) =>
            IO.pure(entry.path -> bytes)
          case None =>
            HttpApiError.raise(
              HttpApiError.internal(s"Problem data manifest references missing storage object: ${entry.path.value}")
            )
        }
      }
      .flatMap(writeZipArchive)

  private def writeZipArchive(files: List[(ProblemDataPath, Array[Byte])]): IO[Array[Byte]] =
    IO.blocking {
      val output = ByteArrayOutputStream()
      val zip = ZipOutputStream(output)
      try
        files.foreach { case (path, bytes) =>
          zip.putNextEntry(ZipEntry(path.value))
          zip.write(bytes)
          zip.closeEntry()
        }
      finally zip.close()

      output.toByteArray
    }

  private def binaryResponse(problemSlug: ProblemSlug, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), "application/zip"),
        Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="${problemSlug.value}-data.zip""""),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])

/** 下载题目数据压缩包的管理端响应 API；只允许题目管理者读取完整数据集。 */
final case class DownloadProblemDataArchive(problemDataStorage: ProblemDataStorageContext)
    extends AuthenticatedResponseApi[ProblemManagementContext]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/archive-downloads")

  /** 解析题目管理上下文；不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  /** 校验管理权限后从清单表和对象存储生成 zip 响应；缺失对象会转为内部错误。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[Response[IO]] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => downloadManagedProblemDataArchive(connection, problem))

  /** 对已确认可管理的题目生成数据归档响应，输出包含 Content-Disposition 的二进制响应。 */
  def downloadManagedProblemDataArchive(connection: Connection, problem: ProblemDetail): IO[Response[IO]] =
    ProblemDataFileTable
      .manifestForProblem(connection, problem.id, problem.slug)
      .flatMap(manifest => archiveResponse(problem.slug, manifest.entries))

  /** 按清单条目读取对象存储并组装 zip；调用方需保证 entries 来自同一题目。 */
  def archiveResponse(problemSlug: ProblemSlug, entries: List[ProblemDataManifestEntry]): IO[Response[IO]] =
    DownloadProblemDataArchive.archiveResponse(
      problemSlug,
      entries,
      (problemSlug, path) => ProblemDataStorage.readPath(problemDataStorage, problemSlug, path)
    )
