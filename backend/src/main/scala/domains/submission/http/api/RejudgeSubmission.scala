package domains.submission.http.api

import domains.submission.http.response.SubmissionHttpResponses



import domains.submission.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.submission.application.SubmissionCommands
import shared.model.PageRequest
import domains.submission.application.input.{CreateSubmissionRequest, SubmissionListRequest}
import domains.submission.model.{SubmissionId, SubmissionProblemQuery, SubmissionSort, SubmissionSortDirection, SubmissionUserQuery, SubmissionVerdictFilter}
import shared.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object RejudgeSubmission:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
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
        userQuery = queryParams.get("username").flatMap(rawQuery => SubmissionUserQuery.parse(rawQuery).toOption),
        problemQuery = queryParams.get("problem").flatMap(rawQuery => SubmissionProblemQuery.parse(rawQuery).toOption),
        verdict = verdict,
        sort = sort,
        direction = direction,
        pageRequest = PageRequest(page = page, pageSize = pageSize)
      )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int, fieldName: String): Either[String, Int] =
    rawValue match
      case None => Right(defaultValue)
      case Some(value) =>
        value.toIntOption match
          case Some(parsed) if parsed > 0 => Right(parsed)
          case _ => Left(s"$fieldName must be a positive integer.")

