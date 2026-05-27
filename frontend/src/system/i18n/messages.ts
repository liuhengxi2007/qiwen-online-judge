import { enMessages } from '@/system/i18n/messages/en'
import { zhCnMessages } from '@/system/i18n/messages/zh-CN'

export type Locale = 'en' | 'zh-CN'

export const fallbackLocale: Locale = 'en'

export const messages: Record<Locale, Record<string, string>> = {
  en: enMessages,
  'zh-CN': zhCnMessages,
}

type TranslateValues = Record<string, string | number>

export function resolveLocale(): Locale {
  if (typeof window !== 'undefined') {
    const persistedLocale = window.localStorage.getItem('qiwen-online-judge.locale')
    if (persistedLocale === 'en' || persistedLocale === 'zh-CN') {
      return persistedLocale
    }
  }

  if (typeof navigator !== 'undefined' && navigator.language.toLowerCase().startsWith('zh')) {
    return 'zh-CN'
  }

  return fallbackLocale
}

export function translateMessage(key: string, values: TranslateValues = {}, locale: Locale = resolveLocale()): string {
  const template = messages[locale][key] ?? messages[fallbackLocale][key] ?? key

  return Object.entries(values).reduce(
    (result, [currentKey, value]) => result.replaceAll(`{{${currentKey}}}`, String(value)),
    template,
  )
}
