import { enMessages } from '@/system/i18n/messages/en'
import { zhCnMessages } from '@/system/i18n/messages/zh-CN'

/**
 * 前端内置支持的界面语言标识，需与消息表目录保持一致。
 */
export type Locale = 'en' | 'zh-CN'

/**
 * 缺失翻译或无法识别浏览器语言时使用的回退语言。
 */
export const fallbackLocale: Locale = 'en'

/**
 * 当前界面语言的持久化 key；读写必须共用同一常量，避免改名时出现漂移。
 */
export const localeStorageKey = 'qiwen-online-judge.locale'

/**
 * 按语言聚合的完整消息表；调用方通过翻译键访问，不直接依赖具体文件结构。
 */
export const messages: Record<Locale, Record<string, string>> = {
  en: enMessages,
  'zh-CN': zhCnMessages,
}

/**
 * 消息模板插值参数，值会在替换前转为字符串。
 */
type TranslateValues = Record<string, string | number>

/**
 * 解析当前首选语言；优先 localStorage，再根据浏览器中文语言回退到 zh-CN，否则使用默认语言。
 */
export function resolveLocale(): Locale {
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

/**
 * 按语言和回退语言查找消息模板并执行简单占位符替换；缺失键时返回 key 便于排查。
 */
export function translateMessage(key: string, values: TranslateValues = {}, locale: Locale = resolveLocale()): string {
  const template = messages[locale][key] ?? messages[fallbackLocale][key] ?? key

  return Object.entries(values).reduce(
    (result, [currentKey, value]) => result.replaceAll(`{{${currentKey}}}`, String(value)),
    template,
  )
}
