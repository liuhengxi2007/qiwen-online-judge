package domains.submission.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.submission.application.SubmissionCommands
import domains.submission.model.{CreateSubmissionRequest, SubmissionId, SubmissionListRequest, SubmissionSort, SubmissionSortDirection, SubmissionVerdictFilter}
import domains.shared.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object SubmissionRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "submissions" =>
        parseListRequest(request.uri.query.params) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(listRequest) =>
            handlers.execute(request, listRequest, SubmissionHttpPlanDefinitions.listSubmissions)

      case request @ POST -> Root / "api" / "submissions" =>
        handlers.executeDecoded[CreateSubmissionRequest, CreateSubmissionRequest, SubmissionCommands.CreateSubmissionResult](
          request,
          SubmissionHttpPlanDefinitions.createSubmission
        )(identity)

      case request @ GET -> Root / "api" / "submissions" / rawSubmissionId =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.getSubmission)

      case request @ POST -> Root / "api" / "submissions" / rawSubmissionId / "delete" =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.deleteSubmission)

      case request @ POST -> Root / "api" / "submissions" / rawSubmissionId / "rejudge" =>
        SubmissionId.parse(rawSubmissionId) match
          case Left(message) =>
            SubmissionHttpResponses.validationErrorResponse(message)
          case Right(submissionId) =>
            handlers.execute(request, submissionId, SubmissionHttpPlanDefinitions.rejudgeSubmission)
    }

  private def parseListRequest(queryParams: Map[String, String]): Either[String, SubmissionListRequest] =
    for
      sort <- queryParams.get("sort") match
        case Some(rawSort) => SubmissionSort.parse(rawSort)
        case None => Right(SubmissionSort.Submitted)
      direction <- queryParams.get("direction") match
        case Some(rawDirection) => SubmissionSortDirection.parse(rawDirection)
        case None => Right(SubmissionSort.defaultDirection(sort))
      verdict <- queryParams.get("verdict") match
        case Some(rawVerdict) => SubmissionVerdictFilter.parse(rawVerdict)
        case None => Right(SubmissionVerdictFilter.All)
      page <- parsePositiveInt(queryParams.get("page"), defaultValue = 1, fieldName = "Page")
      pageSize <- parsePositiveInt(queryParams.get("pageSize"), defaultValue = 10, fieldName = "Page size")
    yield
      SubmissionListRequest(
        userQuery = queryParams.get("username").map(_.trim).filter(_.nonEmpty),
        problemQuery = queryParams.get("problem").map(_.trim).filter(_.nonEmpty),
        verdict = verdict,
        sort = sort,
        direction = direction,
        page = page,
        pageSize = pageSize
      )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int, fieldName: String): Either[String, Int] =
    rawValue match
      case None => Right(defaultValue)
      case Some(value) =>
        value.toIntOption match
          case Some(parsed) if parsed > 0 => Right(parsed)
          case _ => Left(s"$fieldName must be a positive integer.")
