package domains.problemset.http.api



import domains.problemset.http.*
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problem.model.ProblemSlug
import domains.problemset.application.input.{AddProblemToProblemSetRequest, CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSetSlug}
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblemSets:

  def routes(context: ProblemSetHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" =>
        context.handlers.execute(request, PageRequestQuerySupport.parsePageRequest(request.uri.query.params), ProblemSetHttpPlanDefinitions.listProblemSets)
    }