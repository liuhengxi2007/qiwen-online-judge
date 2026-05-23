package domains.user.http

import database.DatabaseSession
import domains.auth.application.SessionStore

final case class UserHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  handlers: UserHttpHandlers
)
