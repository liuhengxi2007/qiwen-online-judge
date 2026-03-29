import { useEffect, useState } from 'react'

import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { useUserSettingsQueryStore } from '@/features/auth/stores/use-user-settings-query-store'

type UseUserSettingsQueryArgs = {
  canLoadTarget: boolean
  targetUsername: string
}

export function useUserSettingsQuery({ canLoadTarget, targetUsername }: UseUserSettingsQueryArgs) {
  const activeTargetUsername = useUserSettingsQueryStore((state) => state.activeTargetUsername)
  const editedUser = useUserSettingsQueryStore((state) => state.editedUser)
  const isLoadingSettings = useUserSettingsQueryStore((state) => state.isLoadingSettings)
  const settingsLoadError = useUserSettingsQueryStore((state) => state.settingsLoadError)
  const loadUserSettings = useUserSettingsQueryStore((state) => state.loadUserSettings)
  const reset = useUserSettingsQueryStore((state) => state.reset)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  useEffect(() => {
    if (!canLoadTarget) {
      reset()
      setNavigationIntent(null)
      return
    }

    let isCancelled = false
    setNavigationIntent(null)

    void loadUserSettings(targetUsername).then((result) => {
      if (isCancelled) {
        return
      }

      if (result.kind === 'forbidden') {
        setNavigationIntent(toSiteManageDeniedRedirect())
      }
    })

    return () => {
      isCancelled = true
    }
  }, [canLoadTarget, loadUserSettings, reset, targetUsername])

  return {
    editedUser: activeTargetUsername === targetUsername ? editedUser : null,
    isLoadingSettings,
    settingsLoadError,
    navigationIntent,
  }
}
