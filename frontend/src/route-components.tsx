import type { ReactElement } from 'react'
import { Navigate } from 'react-router-dom'

import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { useMessageRealtimeConnection } from '@/features/message/hooks/use-message-realtime-connection'
import { useNotificationRealtimeConnection } from '@/features/notification/hooks/use-notification-realtime-connection'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

export function RootRedirect() {
  const session = useAuthStore((state) => state.session)
  return session ? <DashboardPage /> : <Navigate replace to="/login" />
}

export function GuestOnlyRoute({ element }: { element: ReactElement }) {
  const session = useAuthStore((state) => state.session)
  return session ? <Navigate replace to="/" /> : element
}

export function AuthenticatedRoute({ element }: { element: ReactElement }) {
  const session = useAuthStore((state) => state.session)
  useMessageRealtimeConnection()
  useNotificationRealtimeConnection()
  return session ? element : <Navigate replace to="/login" />
}
