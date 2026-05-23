package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses
import domains.problem.http.*
import domains.problem.http.codec.ProblemHttpCodecs.given
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.application.input.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemListRequest, ProblemSearchQuery, UpdateProblemRequest}
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import shared.http.AuthenticatedHttpExecutor
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblems:

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
            pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
          ),
          plans.listProblems
        )
    }
