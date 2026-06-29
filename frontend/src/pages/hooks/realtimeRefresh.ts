import { ListInbox } from '@/apis/message/ListInbox'
import { GetNotificationUnreadCount } from '@/apis/notification/GetNotificationUnreadCount'
import { ListNotifications } from '@/apis/notification/ListNotifications'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { useMessageStore } from '@/pages/stores/message/UseMessageStore'
import { useNotificationStore } from '@/pages/stores/notification/UseNotificationStore'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'

const fallbackInboxLoadError = 'Unable to load messages.'
const fallbackNotificationLoadError = 'Unable to load notifications.'

/** 通过权威列表 API 刷新私信收件箱 store。 */
export async function refreshMessageInbox(pageRequest?: PageRequest) {
  const store = useMessageStore.getState()
  store.beginInboxLoad()
  try {
    store.replaceInbox(await sendAPI(new ListInbox(pageRequest)))
  } catch (error) {
    store.failInboxLoad(isHttpClientError(error) ? error.message : fallbackInboxLoadError)
  }
}

/** 通过权威列表 API 刷新通知列表 store。 */
export async function refreshNotifications(pageRequest?: PageRequest) {
  const store = useNotificationStore.getState()
  store.beginNotificationsLoad()
  try {
    store.replaceNotifications(await sendAPI(new ListNotifications(pageRequest)))
  } catch (error) {
    store.failNotificationsLoad(isHttpClientError(error) ? error.message : fallbackNotificationLoadError)
  }
}

/** 只刷新通知未读数。 */
export async function refreshNotificationUnreadCount() {
  const store = useNotificationStore.getState()
  try {
    const response = await sendAPI(new GetNotificationUnreadCount())
    store.replaceUnreadCount(response.unreadCount)
  } catch {
    store.finishUnreadCountLoad()
  }
}

/** 在可能漏掉实时事件后补齐所有实时驱动的状态。 */
export async function refreshRealtimeState() {
  await Promise.all([
    refreshMessageInbox(),
    refreshNotificationUnreadCount(),
    useNotificationStore.getState().hasLoadedList ? refreshNotifications() : Promise.resolve(),
  ])
}
