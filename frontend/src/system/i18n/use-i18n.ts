import { useContext } from 'react'

import { I18nContext } from '@/system/i18n/i18n-context'

export function useI18n() {
  const context = useContext(I18nContext)

  if (context === null) {
    throw new Error('useI18n must be used within I18nProvider.')
  }

  return context
}
