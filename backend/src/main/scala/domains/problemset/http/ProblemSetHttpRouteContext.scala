package domains.problemset.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthenticatedHttpExecutor

final case class ProblemSetHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: AuthenticatedHttpExecutor
)
