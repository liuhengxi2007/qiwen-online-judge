package domains.usergroup.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import shared.http.AuthenticatedHttpExecutor

final case class UserGroupHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: AuthenticatedHttpExecutor
)
