package domains.problem.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.model.{ProblemDetail, ProblemSummary}
import domains.problem.table.ProblemTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.ProblemCommandSupport.*

object ProblemQueryCommands:

  def listProblems(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    }

  def getProblemBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: domains.problem.model.ProblemSlug
  ): IO[GetProblemResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.findBySlug(connection, slug).flatMap {
        case Some(problem) =>
          enrichProblemPermissions(connection, actor, problem).map {
            case Some(enrichedProblem) => GetProblemResult.Found(enrichedProblem)
            case None => GetProblemResult.Forbidden
          }
        case None =>
          IO.pure(GetProblemResult.NotFound)
      }
    }
