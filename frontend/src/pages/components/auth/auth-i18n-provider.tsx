import type { ReactNode } from 'react'

import { useAuthStore } from '@/pages/objects/auth/use-auth-store'
import { I18nProvider } from '@/system/i18n/i18n'

type AuthI18nProviderProps = {
  children: ReactNode
}

export function AuthI18nProvider({ children }: AuthI18nProviderProps) {
  const sessionLocale = useAuthStore((state) => state.session?.preferences.locale ?? null)

  return <I18nProvider sessionLocale={sessionLocale}>{children}</I18nProvider>
}
