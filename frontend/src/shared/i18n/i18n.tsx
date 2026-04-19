import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { fallbackLocale, messages, type Locale } from '@/shared/i18n/messages'

type TranslateValues = Record<string, string | number>

type I18nContextValue = {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: (key: string, values?: TranslateValues) => string
}

const localeStorageKey = 'qiwen-online-judge.locale'

const I18nContext = createContext<I18nContextValue | null>(null)

function detectInitialLocale(): Locale {
  if (typeof window !== 'undefined') {
    const persistedLocale = window.localStorage.getItem(localeStorageKey)
    if (persistedLocale === 'en' || persistedLocale === 'zh-CN') {
      return persistedLocale
    }
  }

  if (typeof navigator !== 'undefined' && navigator.language.toLowerCase().startsWith('zh')) {
    return 'zh-CN'
  }

  return fallbackLocale
}

function interpolate(template: string, values: TranslateValues = {}): string {
  return Object.entries(values).reduce(
    (result, [key, value]) => result.replaceAll(`{{${key}}}`, String(value)),
    template,
  )
}

function isHiddenDescriptionKey(key: string): boolean {
  return /(^|\.)(description)$/i.test(key) ||
    /(?:Description|Help|Hint|Subheading)$/.test(key)
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const sessionLocale = useAuthStore((state) => state.session?.preferences.locale ?? null)
  const [persistedLocale, setPersistedLocale] = useState<Locale>(detectInitialLocale)
  const locale = sessionLocale ?? persistedLocale

  useEffect(() => {
    document.documentElement.lang = locale
    window.localStorage.setItem(localeStorageKey, locale)
  }, [locale])

  function t(key: string, values?: TranslateValues): string {
    if (isHiddenDescriptionKey(key)) {
      return ''
    }

    const template = messages[locale][key] ?? messages[fallbackLocale][key] ?? key
    return interpolate(template, values)
  }

  return <I18nContext.Provider value={{ locale, setLocale, t }}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const context = useContext(I18nContext)

  if (context === null) {
    throw new Error('useI18n must be used within I18nProvider.')
  }

  return context
}
