import { useRealtimeConnection } from '@/pages/hooks/useRealtimeConnection'

/**
 * 兼容仍挂载旧通知实时 hook 的调用方。
 */
export function useNotificationRealtimeConnection() {
  useRealtimeConnection()
}
