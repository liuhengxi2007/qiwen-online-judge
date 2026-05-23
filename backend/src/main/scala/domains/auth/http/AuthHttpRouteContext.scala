package domains.auth.http

import database.DatabaseSession
import domains.auth.application.SessionStore

final case class AuthHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: AuthHttpHandlers
)
