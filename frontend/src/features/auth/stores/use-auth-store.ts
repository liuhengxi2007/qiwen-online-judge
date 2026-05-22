import { create } from 'zustand'

import type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
import { clearAuthSession, persistAuthSession, readAuthSession } from '@/features/auth/lib/auth-storage'

type AuthStore = {
  session: SessionResponse | null
  setSession: (session: SessionResponse | null) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthStore>()((set) => ({
  session: readAuthSession(),
  setSession: (session) => {
    if (session) {
      persistAuthSession(session)
    } else {
      clearAuthSession()
    }

    set({ session })
  },
  clearSession: () => {
    clearAuthSession()
    set({ session: null })
  },
}))
