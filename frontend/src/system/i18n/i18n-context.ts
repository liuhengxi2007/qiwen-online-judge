import { createContext } from 'react'

import type { Locale } from '@/system/i18n/messages'

/**
 * 翻译模板支持的插值参数集合，键名对应消息中的 {{name}} 占位符。
 */
export type TranslateValues = Record<string, string | number>

/**
 * 国际化上下文对页面暴露的运行时能力：当前语言、切换语言和翻译函数。
 */
export type I18nContextValue = {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: (key: string, values?: TranslateValues) => string
}

/**
 * 国际化 React 上下文；初始为 null，以便 hook 能检测 Provider 缺失。
 */
export const I18nContext = createContext<I18nContextValue | null>(null)
