package domains.problem.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.problem.application.ProblemCommandResults.*
import domains.problem.application.utils.ProblemCommandSupport.*
import domains.problem.application.output.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import domains.problem.application.input.{ProblemListRequest, ProblemSearchQuery}
import domains.problem.table.ProblemTable
import shared.model.PageResponse

object ProblemQueryCommands:
  def listProblems(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: ProblemListRequest
  ): IO[PageResponse[ProblemSummary]] =
    val normalizedRequest = request.copy(pageRequest = request.pageRequest.normalized)
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.listVisibleTo(connection, actor, normalizedRequest)
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

  def listProblemSuggestions(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    query: ProblemSearchQuery
  ): IO[List[ProblemSuggestion]] =
    databaseSession.withTransactionConnection { connection =>
      ProblemTable.listSuggestions(connection, actor, query)
    }
