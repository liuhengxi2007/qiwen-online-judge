import { create } from 'zustand'

import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { clearAuthSession, persistAuthSession, readAuthSession } from '@/pages/stores/auth/AuthSessionStorage'

/**
 * 认证全局状态，保存当前会话并封装持久化写入与清理动作。
 */
type AuthStore = {
  session: SessionResponse | null
  setSession: (session: SessionResponse | null) => void
  clearSession: () => void
}

/**
 * 认证 Zustand store；初始化时读取 localStorage，更新会话时同步持久化状态。
 */
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
