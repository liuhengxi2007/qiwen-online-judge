import { useEffect, useState, type ReactNode } from 'react'

import { localeStorageKey, resolveLocale, translateMessage, type Locale } from '@/system/i18n/messages'
import { I18nContext, type TranslateValues } from '@/system/i18n/i18n-context'

/**
 * 解析初始界面语言，优先读取持久化偏好，再回退到浏览器语言和默认语言。
 */
function detectInitialLocale(): Locale {
  return resolveLocale()
}

/**
 * 识别当前 UI 不展示的说明类翻译键，避免 description/help 类文案在紧凑控件中重复出现。
 */
function isHiddenDescriptionKey(key: string): boolean {
  return /(^|\.)(description)$/i.test(key) ||
    /(?:Description|Help|Hint|Subheading)$/.test(key)
}

/**
 * 国际化 Provider 的输入属性；可由登录会话临时覆盖本地持久化语言。
 */
type I18nProviderProps = {
  children: ReactNode
  sessionLocale?: Locale | null
}

/**
 * 提供当前语言、切换函数和翻译函数；会同步 document.lang 与 localStorage，副作用只随语言变化发生。
 */
export function I18nProvider({ children, sessionLocale = null }: I18nProviderProps) {
  const [persistedLocale, setPersistedLocale] = useState<Locale>(detectInitialLocale)
  const locale = sessionLocale ?? persistedLocale
  const setLocale = (nextLocale: Locale) => {
    setPersistedLocale(nextLocale)
  }

  useEffect(() => {
    document.documentElement.lang = locale
    window.localStorage.setItem(localeStorageKey, locale)
  }, [locale])

  /**
   * 面向组件的翻译入口；对说明类隐藏键返回空串，其余键委托到消息表解析。
   */
  function t(key: string, values?: TranslateValues): string {
    if (isHiddenDescriptionKey(key)) {
      return ''
    }

    return translateMessage(key, values, locale)
  }

  return <I18nContext.Provider value={{ locale, setLocale, t }}>{children}</I18nContext.Provider>
}
