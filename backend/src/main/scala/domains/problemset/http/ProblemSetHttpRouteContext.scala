package domains.problemset.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import shared.http.AuthenticatedHttpExecutor

final case class ProblemSetHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: AuthenticatedHttpExecutor
)
