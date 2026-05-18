import type { ReactNode } from 'react'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { I18nProvider } from '@/shared/i18n/i18n'

type AuthI18nProviderProps = {
  children: ReactNode
}

export function AuthI18nProvider({ children }: AuthI18nProviderProps) {
  const sessionLocale = useAuthStore((state) => state.session?.preferences.locale ?? null)

  return <I18nProvider sessionLocale={sessionLocale}>{children}</I18nProvider>
}
