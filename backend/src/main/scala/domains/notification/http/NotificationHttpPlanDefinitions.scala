package domains.notification.http

import domains.notification.http.mapper.NotificationHttpResponseMappers



import domains.notification.application.NotificationEventHub
import shared.http.AuthenticatedHttpPlanRegistry

object NotificationHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listNotifications: Plain[domains.auth.objects.AuthUser, shared.objects.PageRequest, domains.notification.objects.response.NotificationListResponse],
    getUnreadCount: Plain[domains.auth.objects.AuthUser, Unit, domains.notification.objects.response.NotificationUnreadCountResponse],
    markNotificationRead: WithTransaction[domains.auth.objects.AuthUser, domains.notification.objects.NotificationId, domains.notification.application.NotificationCommands.MarkNotificationReadResult],
    markAllNotificationsRead: WithTransaction[domains.auth.objects.AuthUser, Unit, domains.notification.application.NotificationCommands.MarkAllNotificationsReadResult]
  )

  def plans(notificationEventHub: NotificationEventHub): RegisteredPlans =
    RegisteredPlans(
      listNotifications = Plain(NotificationHttpPlans.ListNotifications, NotificationHttpResponseMappers.listResponse),
      getUnreadCount = Plain(NotificationHttpPlans.GetUnreadCount, NotificationHttpResponseMappers.unreadCountResponse),
      markNotificationRead = WithTransaction(new NotificationHttpPlans.MarkNotificationReadPlan(notificationEventHub), NotificationHttpResponseMappers.markReadResponse),
      markAllNotificationsRead = WithTransaction(new NotificationHttpPlans.MarkAllNotificationsReadPlan(notificationEventHub), NotificationHttpResponseMappers.markAllReadResponse)
    )
