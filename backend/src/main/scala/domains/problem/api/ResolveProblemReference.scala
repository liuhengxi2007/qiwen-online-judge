package domains.problem.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.{ProblemReference, ProblemSlug}
import domains.problem.objects.response.ResolveProblemReferenceResponse
import domains.problem.table.problem.ProblemQueryTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object ResolveProblemReference extends InternalOnlyApi[ProblemSlug, ResolveProblemReferenceResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/resolve-reference")

  override def plan(connection: Connection, slug: ProblemSlug): IO[ResolveProblemReferenceResponse] =
    ProblemQueryTable
      .findBySlug(connection, slug)
      .map(problem => ResolveProblemReferenceResponse(problem.map(problem => ProblemReference(problem.id, problem.slug, problem.title))))
