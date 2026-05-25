package domains.notification.http

import domains.notification.http.mapper.NotificationHttpResponseMappers



import domains.notification.application.NotificationEventHub
import shared.http.AuthenticatedHttpPlanRegistry

object NotificationHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listNotifications: Plain[domains.auth.model.AuthUser, shared.model.PageRequest, domains.notification.model.response.NotificationListResponse],
    getUnreadCount: Plain[domains.auth.model.AuthUser, Unit, domains.notification.model.response.NotificationUnreadCountResponse],
    markNotificationRead: WithTransaction[domains.auth.model.AuthUser, domains.notification.model.NotificationId, domains.notification.application.NotificationCommands.MarkNotificationReadResult],
    markAllNotificationsRead: WithTransaction[domains.auth.model.AuthUser, Unit, domains.notification.application.NotificationCommands.MarkAllNotificationsReadResult]
  )

  def plans(notificationEventHub: NotificationEventHub): RegisteredPlans =
    RegisteredPlans(
      listNotifications = Plain(NotificationHttpPlans.ListNotifications, NotificationHttpResponseMappers.listResponse),
      getUnreadCount = Plain(NotificationHttpPlans.GetUnreadCount, NotificationHttpResponseMappers.unreadCountResponse),
      markNotificationRead = WithTransaction(new NotificationHttpPlans.MarkNotificationReadPlan(notificationEventHub), NotificationHttpResponseMappers.markReadResponse),
      markAllNotificationsRead = WithTransaction(new NotificationHttpPlans.MarkAllNotificationsReadPlan(notificationEventHub), NotificationHttpResponseMappers.markAllReadResponse)
    )
