import { useContext } from 'react'

import { I18nContext } from '@/system/i18n/i18n-context'

/**
 * 读取国际化上下文；必须在 I18nProvider 内调用，缺失 Provider 时抛出配置错误。
 */
export function useI18n() {
  const context = useContext(I18nContext)

  if (context === null) {
    throw new Error('useI18n must be used within I18nProvider.')
  }

  return context
}
