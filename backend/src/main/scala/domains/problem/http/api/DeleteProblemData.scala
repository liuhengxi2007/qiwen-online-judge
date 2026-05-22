package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses



import domains.problem.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.application.input.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemListRequest, UpdateProblemRequest}
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSearchQuery, ProblemSlug}
import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import shared.model.PageRequest
import shared.http.AuthenticatedHttpExecutor
import fs2.text
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.multipart.Multipart

object DeleteProblemData:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = ProblemHttpPlanDefinitions.plans(problemDataStorage)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / filename / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                handlers.execute(request, (parsedProblemSlug, parsedFilename), plans.deleteProblemData)
    }

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)

  private def uploadMultipartProblemDataFile(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    problemDataStorage: ProblemDataStorage,
    request: org.http4s.Request[IO],
    problemSlug: ProblemSlug
  ): IO[org.http4s.Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Multipart[IO]].flatMap { multipart =>
        extractNamedBinaryPart(multipart, "file").flatMap {
          case None =>
            ProblemHttpResponses.validationErrorResponse("Multipart file field 'file' is required.")
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
                ProblemHttpResponses.validationErrorResponse(error.getMessage)
              case Left(error) =>
                IO.raiseError(error)
              case Right(maybePath) =>
                val resolvedPath = maybePath.orElse(filePart.filename.flatMap(name => ProblemDataPath.parse(name).toOption))
                resolvedPath match
                  case None =>
                    ProblemHttpResponses.validationErrorResponse("Multipart upload requires a valid 'path' field or uploaded filename.")
                  case Some(path) =>
                    databaseSession.withTransactionConnection(connection =>
                      ProblemCommands
                        .uploadProblemDataFile(problemDataStorage, connection, actor, problemSlug, path, bytes)
                        .flatMap(ProblemHttpResponses.mapUpdateDataResult)
                    )
            }
        }
      }
    }

  private def uploadMultipartProblemDataArchive(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    problemDataStorage: ProblemDataStorage,
    request: org.http4s.Request[IO],
    problemSlug: ProblemSlug
  ): IO[org.http4s.Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Multipart[IO]].flatMap { multipart =>
        extractNamedBinaryPart(multipart, "file").flatMap {
          case None =>
            ProblemHttpResponses.validationErrorResponse("Multipart file field 'file' is required.")
          case Some((filePart, bytes)) =>
            val validArchiveName = filePart.filename.exists(_.toLowerCase.endsWith(".zip"))
            if !validArchiveName then
              ProblemHttpResponses.validationErrorResponse("Multipart archive upload requires a .zip file.")
            else
              extractOptionalPathField(multipart, "targetDir").attempt.flatMap {
                case Left(error: IllegalArgumentException) =>
                  ProblemHttpResponses.validationErrorResponse(error.getMessage)
                case Left(error) =>
                  IO.raiseError(error)
                case Right(targetDirectory) =>
                  databaseSession.withTransactionConnection(connection =>
                    ProblemCommands
                      .uploadProblemDataArchive(problemDataStorage, connection, actor, problemSlug, targetDirectory, bytes)
                      .flatMap(ProblemHttpResponses.mapUpdateDataResult)
                  )
              }
        }
      }
    }

  private def extractNamedBinaryPart(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[(org.http4s.multipart.Part[IO], Array[Byte])]] =
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

  private def decodeTextPart(part: org.http4s.multipart.Part[IO]): IO[String] =
    part.body.through(text.utf8.decode).compile.string

