package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.model.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemDataFilename, ProblemDataPath, ProblemListRequest, ProblemSearchQuery, ProblemSlug, UpdateProblemRequest}
import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import domains.shared.model.PageRequest
import domains.shared.http.AuthenticatedHttpExecutor
import fs2.text
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.multipart.Multipart

object ProblemRouter:

  private object PathQueryParamMatcher extends org.http4s.dsl.impl.QueryParamDecoderMatcher[String]("path")

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, problemDataStorage: ProblemDataStorage): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = ProblemHttpPlanDefinitions.plans(problemDataStorage)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        handlers.execute(
          request,
          ProblemListRequest(
            query = request.uri.query.params.get("q").flatMap(rawQuery => ProblemSearchQuery.parse(rawQuery).toOption),
            pageRequest = PageRequest(
              page = parsePositiveInt(request.uri.query.params.get("page"), 1),
              pageSize = parsePositiveInt(request.uri.query.params.get("pageSize"), 10)
            )
          ),
          plans.listProblems
        )

      case request @ GET -> Root / "api" / "problems" / "suggestions" =>
        ProblemSearchQuery.parse(request.uri.query.params.get("q").getOrElse("")) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(query) =>
            handlers.execute(request, query, plans.listProblemSuggestions)

      case request @ POST -> Root / "api" / "problems" =>
        handlers.executeDecoded[CreateProblemRequest, CreateProblemRequest, ProblemCommands.CreateProblemResult](
          request,
          plans.createProblem
        )(identity)

      case request @ GET -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, plans.getProblem)

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, plans.listProblemData)

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" / "tree" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, plans.listProblemDataTree)

      case GET -> Root / "api" / "problems" / problemSlug / "data" / "file" :? PathQueryParamMatcher(rawPath) =>
        (ProblemSlug.parse(problemSlug), ProblemDataPath.parse(rawPath)) match
          case (Left(message), _) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case (_, Left(message)) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case (Right(parsedProblemSlug), Right(parsedPath)) =>
            ProblemHttpResponses.downloadDataPathResponse(problemDataStorage, parsedProblemSlug, parsedPath)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "file" / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.executeDecoded[DeleteProblemDataPathRequest, (ProblemSlug, DeleteProblemDataPathRequest), ProblemCommands.DeleteProblemDataResult](
              request,
              plans.deleteProblemDataPath
            ) { deleteRequest => (parsedProblemSlug, deleteRequest) }

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" / filename =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                handlers.execute(request, (parsedProblemSlug, parsedFilename), plans.downloadProblemData)

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

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "clear" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, plans.clearProblemData)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "ready" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.executeDecoded[SetProblemReadyRequest, (ProblemSlug, SetProblemReadyRequest), ProblemCommands.SetProblemReadyResult](
              request,
              plans.setProblemReady
            ) { readyRequest => (parsedProblemSlug, readyRequest) }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "files" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            uploadMultipartProblemDataFile(databaseSession, sessionStore, problemDataStorage, request, parsedProblemSlug)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "archive" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            uploadMultipartProblemDataArchive(databaseSession, sessionStore, problemDataStorage, request, parsedProblemSlug)

      case request @ POST -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.executeDecoded[UpdateProblemRequest, (ProblemSlug, UpdateProblemRequest), ProblemCommands.UpdateProblemResult](
              request,
              plans.updateProblem
            ) {
              updateRequest => (parsedProblemSlug, updateRequest)
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, plans.deleteProblem)
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
