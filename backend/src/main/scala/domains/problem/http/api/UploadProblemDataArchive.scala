package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.utils.{ProblemDataStorage, ProblemDataUploadPreparation}
import domains.problem.http.ProblemDataApiSupport
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.response.ProblemDataUploadResult
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import fs2.text
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class UploadProblemDataArchive(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemSlug, Option[ProblemDataPath], Array[Byte]), ProblemDataUploadResult]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/archive")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataUploadResult] = summon[Encoder[ProblemDataUploadResult]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, Option[ProblemDataPath], Array[Byte])] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
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
    yield (problemSlug, targetDirectory, bytes)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSlug, Option[ProblemDataPath], Array[Byte])
  ): IO[ProblemDataUploadResult] =
    val (problemSlug, targetDirectory, bytes) = input
    ProblemDataUploadPreparation.prepareArchive(bytes, targetDirectory) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(preparedFiles) =>
        ProblemDataApiSupport.summaryFilenameFor(preparedFiles) match
          case Left(message) =>
            HttpApiError.raise(HttpApiError.badRequest(message))
          case Right(summaryFilename) =>
            ProblemDataApiSupport.withManageableProblemForUpdate(connection, actor, problemSlug) { problem =>
              for
                snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
                uploadResult <- ProblemDataApiSupport
                  .writePreparedFiles(problemDataStorage, problem.slug, preparedFiles)
                  .flatMap(_ => ProblemDataStateTable.updateData(connection, problem.id, Instant.now(), summaryFilename))
                  .flatMap(_ => ProblemDataFileTable.upsertForProblem(connection, problem.id, ProblemDataApiSupport.toManifestEntries(preparedFiles), Instant.now()))
                  .flatMap(_ => ProblemDataApiSupport.refreshedManagedProblem(connection, problem, "Problem disappeared after data update."))
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
      case Some(part) => part.body.compile.to(Array).map(bytes => Some((part, bytes)))

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
    part.body.through(text.utf8.decode).compile.string
