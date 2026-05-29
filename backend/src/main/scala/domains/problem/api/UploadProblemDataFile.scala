package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
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
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class UploadProblemDataFile(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ProblemSlug, ProblemDataPath, Array[Byte]), ProblemDataUploadResult]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataUploadResult] = summon[Encoder[ProblemDataUploadResult]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSlug, ProblemDataPath, Array[Byte])] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
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
    yield (problemSlug, resolvedPath, bytes)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (ProblemSlug, ProblemDataPath, Array[Byte])
  ): IO[ProblemDataUploadResult] =
    val (problemSlug, path, bytes) = input
    ProblemDataUploadPreparation.prepareSingleFile(path, bytes) match
      case Left(message) =>
        HttpApiError.raise(HttpApiError.badRequest(message))
      case Right(preparedFile) =>
        ProblemDataApiHelpers.summaryFilenameFor(List(preparedFile)) match
          case Left(message) =>
            HttpApiError.raise(HttpApiError.badRequest(message))
          case Right(summaryFilename) =>
            EvaluateProblemAccess.plan(connection, actor, problemSlug).flatMap { access =>
              access.problem match
                case None =>
                  HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
                case Some(_) =>
                  HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.problemNotFound)) *>
                    ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
                      for
                        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
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
                            problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
                          }
                      yield uploadResult
                    }
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
