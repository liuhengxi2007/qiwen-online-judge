package domains.problemset.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problemset.model.ProblemSetSummary
import domains.problemset.table.ProblemSetTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.problemset.application.ProblemSetCommandResults.*
import domains.problemset.application.ProblemSetCommandSupport.*

object ProblemSetQueryCommands:

  def listProblemSets(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSetSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      ProblemSetTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    }

  def getProblemSetBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: domains.problemset.model.ProblemSetSlug
  ): IO[GetProblemSetResult] =
    databaseSession.withTransactionConnection { connection =>
      ProblemSetTable.findBySlug(connection, slug).flatMap {
        case Some(problemSet) =>
          canViewProblemSet(connection, actor, problemSet).map {
            case true => GetProblemSetResult.Found(problemSet)
            case false => GetProblemSetResult.Forbidden
          }
        case None =>
          IO.pure(GetProblemSetResult.NotFound)
      }
    }
