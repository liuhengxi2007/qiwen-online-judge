package domains.problem.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.{ProblemReference, ProblemSlug}
import domains.problem.objects.response.ResolveProblemReferenceResponse
import domains.problem.table.problem.ProblemQueryTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部题目引用解析 API；给其它域按 slug 获取稳定的题目 id、slug 与标题三元组。 */
object ResolveProblemReference extends InternalOnlyApi[ProblemSlug, ResolveProblemReferenceResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/resolve-reference")

  /** 按 slug 查询题目引用；不存在时返回 problem=None 而不是抛错。 */
  override def plan(connection: Connection, slug: ProblemSlug): IO[ResolveProblemReferenceResponse] =
    ProblemQueryTable
      .findBySlug(connection, slug)
      .map(problem => ResolveProblemReferenceResponse(problem.map(problem => ProblemReference(problem.id, problem.slug, problem.title))))
