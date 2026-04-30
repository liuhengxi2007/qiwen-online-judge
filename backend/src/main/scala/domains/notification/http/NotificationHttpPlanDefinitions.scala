package domains.notification.http

import domains.notification.application.NotificationEventHub
import domains.shared.http.AuthenticatedHttpPlanRegistry

object NotificationHttpPlanDefinitions:
  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  def plans(notificationEventHub: NotificationEventHub): Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      Plain(NotificationHttpPlans.ListNotifications, NotificationHttpResponses.listResponse),
      Plain(NotificationHttpPlans.GetUnreadCount, NotificationHttpResponses.unreadCountResponse),
      WithTransaction(new NotificationHttpPlans.MarkNotificationReadPlan(notificationEventHub), NotificationHttpResponses.markReadResponse),
      WithTransaction(new NotificationHttpPlans.MarkAllNotificationsReadPlan(notificationEventHub), NotificationHttpResponses.markAllReadResponse)
    ).map(plan => plan.name -> plan).toMap
