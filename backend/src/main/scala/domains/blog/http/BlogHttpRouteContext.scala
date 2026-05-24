package domains.blog.http

import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.notification.application.NotificationEventHub
import domains.auth.http.AuthenticatedHttpExecutor

final case class BlogHttpRouteContext(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  notificationEventHub: NotificationEventHub,
  handlers: AuthenticatedHttpExecutor,
  plans: BlogHttpPlanDefinitions.RegisteredPlans
)
