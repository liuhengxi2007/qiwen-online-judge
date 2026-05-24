package domains.notification.http

import domains.notification.http.response.NotificationHttpResponses



import domains.notification.application.NotificationEventHub
import shared.http.AuthenticatedHttpPlanRegistry

object NotificationHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listNotifications: Plain[domains.auth.model.AuthUser, shared.model.PageRequest, domains.notification.application.output.NotificationListResponse],
    getUnreadCount: Plain[domains.auth.model.AuthUser, Unit, domains.notification.application.output.NotificationUnreadCountResponse],
    markNotificationRead: WithTransaction[domains.auth.model.AuthUser, domains.notification.model.NotificationId, domains.notification.application.NotificationCommands.MarkNotificationReadResult],
    markAllNotificationsRead: WithTransaction[domains.auth.model.AuthUser, Unit, domains.notification.application.NotificationCommands.MarkAllNotificationsReadResult]
  )

  def plans(notificationEventHub: NotificationEventHub): RegisteredPlans =
    RegisteredPlans(
      listNotifications = Plain(NotificationHttpPlans.ListNotifications, NotificationHttpResponses.listResponse),
      getUnreadCount = Plain(NotificationHttpPlans.GetUnreadCount, NotificationHttpResponses.unreadCountResponse),
      markNotificationRead = WithTransaction(new NotificationHttpPlans.MarkNotificationReadPlan(notificationEventHub), NotificationHttpResponses.markReadResponse),
      markAllNotificationsRead = WithTransaction(new NotificationHttpPlans.MarkAllNotificationsReadPlan(notificationEventHub), NotificationHttpResponses.markAllReadResponse)
    )
