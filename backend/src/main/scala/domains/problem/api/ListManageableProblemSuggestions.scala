package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.objects.request.ProblemSearchQuery
import domains.problem.objects.response.ProblemSuggestion
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object ListManageableProblemSuggestions extends AuthenticatedApi[(Option[ContestSlug], ProblemSearchQuery), List[ProblemSuggestion]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problem-suggestions/manageable")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[ProblemSuggestion]] = summon[Encoder[List[ProblemSuggestion]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(Option[ContestSlug], ProblemSearchQuery)] =
    val _ = pathParams
    for
      contestSlug <- ProblemManagementContext.parseOptionalContestSlug(request)
      query <- HttpApiError.fromEitherBadRequest(ProblemSearchQuery.parse(request.uri.query.params.getOrElse("q", "")))
    yield (contestSlug, query)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (Option[ContestSlug], ProblemSearchQuery)
  ): IO[List[ProblemSuggestion]] =
    val (contestSlug, query) = input
    for
      _ <- ProblemManagementContext.requireContestManagementIfPresent(connection, actor, contestSlug)
      suggestions <- ProblemQueryTable.listManageableSuggestions(connection, actor, query)
    yield suggestions
