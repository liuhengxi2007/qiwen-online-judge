package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.request.ProblemSearchQuery
import domains.problem.objects.response.ProblemSuggestion
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object ListProblemSuggestions extends AuthenticatedApi[ProblemSearchQuery, List[ProblemSuggestion]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/suggestions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[ProblemSuggestion]] = summon[Encoder[List[ProblemSuggestion]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSearchQuery] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(ProblemSearchQuery.parse(request.uri.query.params.getOrElse("q", "")))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    query: ProblemSearchQuery
  ): IO[List[ProblemSuggestion]] =
    ProblemQueryTable.listSuggestions(connection, actor, query)
