package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses
import domains.problem.http.*
import domains.problem.http.codec.ProblemHttpCodecs.given
import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.application.input.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemListRequest, ProblemSearchQuery, UpdateProblemRequest}
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblems:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        context.handlers.execute(
          request,
          ProblemListRequest(
            query = request.uri.query.params.get("q").flatMap(rawQuery => ProblemSearchQuery.parse(rawQuery).toOption),
            pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
          ),
          context.plans.listProblems
        )
    }
