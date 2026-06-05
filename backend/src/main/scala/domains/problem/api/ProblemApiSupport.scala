package domains.problem.api

import cats.effect.IO
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemQueryTable
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

object ProblemApiSupport:

  def loadProblemBySlug(connection: Connection, problemSlug: ProblemSlug): IO[ProblemDetail] =
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case Some(problem) => IO.pure(problem)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
    }
