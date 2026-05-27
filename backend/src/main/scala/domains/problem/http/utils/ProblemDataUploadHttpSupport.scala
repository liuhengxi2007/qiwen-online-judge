package domains.problem.http.utils

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.http.mapper.ProblemHttpResponseMappers
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import fs2.text
import org.http4s.{Request, Response}
import org.http4s.multipart.{Multipart, Part}

object ProblemDataUploadHttpSupport:

  def uploadMultipartProblemDataFile(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    problemDataStorage: ProblemDataStorage,
    request: Request[IO],
    problemSlug: ProblemSlug
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Multipart[IO]].flatMap { multipart =>
        extractNamedBinaryPart(multipart, "file").flatMap {
          case None =>
            ProblemHttpResponseMappers.validationErrorResponse("Multipart file field 'file' is required.")
          case Some((filePart, bytes)) =>
            val candidatePath =
              multipart.parts.find(_.name.contains("path")) match
                case Some(pathPart) =>
                  decodeTextPart(pathPart).flatMap {
                    case "" =>
                      IO.pure(None)
                    case rawPath =>
                      ProblemDataPath.parse(rawPath) match
                        case Left(message) => IO.raiseError(IllegalArgumentException(message))
                        case Right(path) => IO.pure(Some(path))
                  }
                case None =>
                  IO.pure(None)

            candidatePath.attempt.flatMap {
              case Left(error: IllegalArgumentException) =>
                ProblemHttpResponseMappers.validationErrorResponse(error.getMessage)
              case Left(error) =>
                IO.raiseError(error)
              case Right(maybePath) =>
                val resolvedPath = maybePath.orElse(filePart.filename.flatMap(name => ProblemDataPath.parse(name).toOption))
                resolvedPath match
                  case None =>
                    ProblemHttpResponseMappers.validationErrorResponse("Multipart upload requires a valid 'path' field or uploaded filename.")
                  case Some(path) =>
                    databaseSession.withTransactionConnection(connection =>
                      ProblemCommands
                        .uploadProblemDataFile(problemDataStorage, connection, actor, problemSlug, path, bytes)
                        .flatMap(ProblemHttpResponseMappers.mapUpdateDataResult)
                    )
            }
        }
      }
    }

  def uploadMultipartProblemDataArchive(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    problemDataStorage: ProblemDataStorage,
    request: Request[IO],
    problemSlug: ProblemSlug
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Multipart[IO]].flatMap { multipart =>
        extractNamedBinaryPart(multipart, "file").flatMap {
          case None =>
            ProblemHttpResponseMappers.validationErrorResponse("Multipart file field 'file' is required.")
          case Some((filePart, bytes)) =>
            val validArchiveName = filePart.filename.exists(_.toLowerCase.endsWith(".zip"))
            if !validArchiveName then
              ProblemHttpResponseMappers.validationErrorResponse("Multipart archive upload requires a .zip file.")
            else
              extractOptionalPathField(multipart, "targetDir").attempt.flatMap {
                case Left(error: IllegalArgumentException) =>
                  ProblemHttpResponseMappers.validationErrorResponse(error.getMessage)
                case Left(error) =>
                  IO.raiseError(error)
                case Right(targetDirectory) =>
                  databaseSession.withTransactionConnection(connection =>
                    ProblemCommands
                      .uploadProblemDataArchive(problemDataStorage, connection, actor, problemSlug, targetDirectory, bytes)
                      .flatMap(ProblemHttpResponseMappers.mapUpdateDataResult)
                  )
              }
        }
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
          else
            ProblemDataPath.parse(normalized) match
              case Left(message) => IO.raiseError(IllegalArgumentException(message))
              case Right(path) => IO.pure(Some(path))
        }

  private def decodeTextPart(part: Part[IO]): IO[String] =
    part.body.through(text.utf8.decode).compile.string
