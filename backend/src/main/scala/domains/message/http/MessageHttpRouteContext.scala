package domains.message.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.message.application.MessageEventHub
import shared.http.AuthenticatedHttpExecutor

final case class MessageHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  messageEventHub: MessageEventHub,
  handlers: AuthenticatedHttpExecutor,
  plans: MessageHttpPlanDefinitions.RegisteredPlans
)
