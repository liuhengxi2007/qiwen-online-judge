package domains.submission.http.api

import cats.effect.IO
import domains.submission.http.*
import domains.submission.http.utils.SubmissionListRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListSubmissions:

  def routes(context: SubmissionHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "submissions" =>
        val listRequest = SubmissionListRequestQuerySupport.parseListRequest(request.uri.query.params)
        context.handlers.execute(request, listRequest, SubmissionHttpPlanDefinitions.listSubmissions)
    }
