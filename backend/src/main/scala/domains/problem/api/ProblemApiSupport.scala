package domains.problem.api

import cats.effect.IO
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemQueryTable
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

/** 题目 API 的共享读取辅助；集中处理按 slug 加载题目和统一 not-found 错误；API 对齐例外：这是后端题目加载支持代码，不是前端端点。 */
object ProblemApiSupport:

  /** 按 slug 加载题目详情；不存在时抛题目不存在错误，不做权限判断。 */
  def loadProblemBySlug(connection: Connection, problemSlug: ProblemSlug): IO[ProblemDetail] =
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case Some(problem) => IO.pure(problem)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
    }
