package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.ProblemSlug
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problem.utils.ProblemDataStorage
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.util.zip.{ZipEntry, ZipOutputStream}

final case class DownloadProblemDataArchive(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedResponseApi[ProblemManagementContext]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/archive-downloads")

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[Response[IO]] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => downloadManagedProblemDataArchive(connection, problem))

  def downloadManagedProblemDataArchive(connection: Connection, problem: ProblemDetail): IO[Response[IO]] =
    ProblemDataFileTable
      .manifestForProblem(connection, problem.id, problem.slug)
      .flatMap(manifest => archiveResponse(problem.slug, manifest.entries))

  def archiveResponse(problemSlug: ProblemSlug, entries: List[ProblemDataManifestEntry]): IO[Response[IO]] =
    archiveBytes(problemSlug, entries).map(bytes => binaryResponse(problemSlug, bytes))

  private def archiveBytes(problemSlug: ProblemSlug, entries: List[ProblemDataManifestEntry]): IO[Array[Byte]] =
    entries
      .sortBy(_.path.value)
      .traverse { entry =>
        problemDataStorage.readPath(problemSlug, entry.path).flatMap {
          case Some((_, bytes)) =>
            IO.pure(entry.path -> bytes)
          case None =>
            HttpApiError.raise(
              HttpApiError.internal(s"Problem data manifest references missing storage object: ${entry.path.value}")
            )
        }
      }
      .flatMap(writeZipArchive)

  private def writeZipArchive(files: List[(domains.problem.objects.ProblemDataPath, Array[Byte])]): IO[Array[Byte]] =
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
