import { useEffect, useState, type ReactNode } from 'react'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { resolveLocale, translateMessage, type Locale } from '@/shared/i18n/messages'
import { I18nContext, type TranslateValues } from '@/shared/i18n/i18n-context'

const localeStorageKey = 'qiwen-online-judge.locale'

function detectInitialLocale(): Locale {
  return resolveLocale()
}

function isHiddenDescriptionKey(key: string): boolean {
  return /(^|\.)(description)$/i.test(key) ||
    /(?:Description|Help|Hint|Subheading)$/.test(key)
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const sessionLocale = useAuthStore((state) => state.session?.preferences.locale ?? null)
  const [persistedLocale, setPersistedLocale] = useState<Locale>(detectInitialLocale)
  const locale = sessionLocale ?? persistedLocale
  const setLocale = (nextLocale: Locale) => {
    setPersistedLocale(nextLocale)
  }

  useEffect(() => {
    document.documentElement.lang = locale
    window.localStorage.setItem(localeStorageKey, locale)
  }, [locale])

  function t(key: string, values?: TranslateValues): string {
    if (isHiddenDescriptionKey(key)) {
      return ''
    }

    return translateMessage(key, values, locale)
  }

  return <I18nContext.Provider value={{ locale, setLocale, t }}>{children}</I18nContext.Provider>
}
