package domains.submission.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthenticatedHttpExecutor

final case class SubmissionHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: AuthenticatedHttpExecutor
)
