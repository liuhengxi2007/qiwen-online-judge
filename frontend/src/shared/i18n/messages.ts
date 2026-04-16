import { enMessages } from '@/shared/i18n/messages/en'
import { zhCnMessages } from '@/shared/i18n/messages/zh-CN'

export type Locale = 'en' | 'zh-CN'

export const fallbackLocale: Locale = 'en'

export const messages: Record<Locale, Record<string, string>> = {
  en: enMessages,
  'zh-CN': zhCnMessages,
}
