import { useRealtimeConnection } from '@/pages/hooks/useRealtimeConnection'

/**
 * 为消息页面重新导出消息实时事件契约。
 */
export { messageStreamEventName, type MessageStreamEventDetail } from '@/pages/hooks/messageRealtimeEvents'

/**
 * 兼容仍挂载旧消息实时 hook 的调用方。
 */
export function useMessageRealtimeConnection() {
  useRealtimeConnection()
}
