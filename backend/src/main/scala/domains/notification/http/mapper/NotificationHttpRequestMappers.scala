package domains.notification.http.mapper

import domains.notification.objects.NotificationId
import shared.http.utils.PageRequestQuerySupport
import shared.objects.PageRequest

object NotificationHttpRequestMappers:

  def unit: Unit = ()

  def listNotificationsRequest(queryParams: Map[String, String]): PageRequest =
    PageRequestQuerySupport.parsePageRequest(queryParams)

  def notificationId(rawNotificationId: String): Either[String, NotificationId] =
    NotificationId.parse(rawNotificationId)
