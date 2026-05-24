package domains.problem.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.problem.application.ProblemDataStorage
import domains.auth.http.AuthenticatedHttpExecutor

final case class ProblemHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  problemDataStorage: ProblemDataStorage,
  handlers: AuthenticatedHttpExecutor,
  plans: ProblemHttpPlanDefinitions.RegisteredPlans
)
