package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser

import domains.problemset.objects.response.ProblemSetSummary
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

object ListProblemSets extends AuthenticatedApi[PageRequest, PageResponse[ProblemSetSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problem-sets")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ProblemSetSummary]] = summon[Encoder[PageResponse[ProblemSetSummary]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSetSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    ProblemSetTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
