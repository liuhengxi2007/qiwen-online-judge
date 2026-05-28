package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.request.{ProblemListRequest, ProblemSearchQuery}
import domains.problem.objects.response.ProblemSummary
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.utils.PageRequestQuerySupport
import shared.http.{ApiPath, PathParams}
import shared.objects.PageResponse

import java.sql.Connection

object ListProblems extends AuthenticatedApi[ProblemListRequest, PageResponse[ProblemSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ProblemSummary]] = summon[Encoder[PageResponse[ProblemSummary]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemListRequest] =
    val _ = pathParams
    val queryParams = request.uri.query.params
    IO.pure(
      ProblemListRequest(
        query = queryParams.get("q").flatMap(rawQuery => ProblemSearchQuery.parse(rawQuery).toOption),
        pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
      )
    )

  override def plan(
    connection: Connection,
    actor: AuthUser,
    request: ProblemListRequest
  ): IO[PageResponse[ProblemSummary]] =
    val normalizedRequest = request.copy(pageRequest = request.pageRequest.normalized)
    ProblemQueryTable.listVisibleTo(connection, actor, normalizedRequest)
