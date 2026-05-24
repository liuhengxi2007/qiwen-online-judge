package domains.usergroup.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthenticatedHttpExecutor

final case class UserGroupHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: AuthenticatedHttpExecutor
)
