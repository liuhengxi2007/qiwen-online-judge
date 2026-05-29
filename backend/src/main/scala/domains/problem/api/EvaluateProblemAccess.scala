package domains.problem.api

import cats.effect.IO
import domains.auth.api.InternalOnlyAuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemAccessEvaluationResponse
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object EvaluateProblemAccess extends InternalOnlyAuthenticatedApi[ProblemSlug, ProblemAccessEvaluationResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/evaluate-access")

  override def plan(connection: Connection, actor: AuthUser, slug: ProblemSlug): IO[ProblemAccessEvaluationResponse] =
    ProblemQueryTable.findBySlug(connection, slug).flatMap {
      case None =>
        IO.pure(ProblemAccessEvaluationResponse(problem = None, canView = false, canManage = false))
      case Some(problem) =>
        ProblemAccessRules.evaluateProblemPermissions(connection, actor, problem).map { access =>
          ProblemAccessEvaluationResponse(problem = Some(problem), canView = access.canView, canManage = access.canManage)
        }
    }
