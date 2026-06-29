package domains.problemset.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.response.ResolveProblemSetSlugResponse
import domains.problemset.table.problem_set.ProblemSetTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部题单 slug 解析 API，用于跨 domain 判断题单 slug 是否已存在。 */
object ResolveProblemSetSlug extends InternalOnlyApi[ProblemSetSlug, ResolveProblemSetSlugResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problem-sets/resolve-slug")

  /** 查找题单 slug 并返回存在标记，不暴露题单详情。 */
  override def plan(connection: Connection, slug: ProblemSetSlug): IO[ResolveProblemSetSlugResponse] =
    ProblemSetTable
      .findBySlug(connection, slug)
      .map(problemSet => ResolveProblemSetSlugResponse(problemSet.nonEmpty))
