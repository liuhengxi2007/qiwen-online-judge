package domains.problemset.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.response.ResolveProblemSetSlugResponse
import domains.problemset.table.problem_set.ProblemSetTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object ResolveProblemSetSlug extends InternalOnlyApi[ProblemSetSlug, ResolveProblemSetSlugResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problem-sets/resolve-slug")

  override def plan(connection: Connection, slug: ProblemSetSlug): IO[ResolveProblemSetSlugResponse] =
    ProblemSetTable
      .findBySlug(connection, slug)
      .map(problemSet => ResolveProblemSetSlugResponse(problemSet.nonEmpty))
