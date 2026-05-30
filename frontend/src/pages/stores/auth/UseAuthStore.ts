import { create } from 'zustand'

import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { clearAuthSession, persistAuthSession, readAuthSession } from '@/pages/stores/auth/AuthSessionStorage'

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
