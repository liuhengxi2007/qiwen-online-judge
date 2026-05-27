import { createContext } from 'react'

import type { Locale } from '@/system/i18n/messages'

export type TranslateValues = Record<string, string | number>

export type I18nContextValue = {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: (key: string, values?: TranslateValues) => string
}

export const I18nContext = createContext<I18nContextValue | null>(null)
