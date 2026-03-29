import { create } from 'zustand'

import type { AuthSession } from '@/domain/auth'
import { clearAuthSession, persistAuthSession, readAuthSession } from '@/lib/auth-storage'

type AuthStore = {
  session: AuthSession | null
  setSession: (session: AuthSession | null) => void
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
