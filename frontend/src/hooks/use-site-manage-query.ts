import { useEffect, useState } from 'react'

import type { NavigationIntent } from '@/lib/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/lib/route-policy'
import { useUserDirectoryStore } from '@/stores/use-user-directory-store'

export function useSiteManageQuery(siteManagerEnabled: boolean) {
  const users = useUserDirectoryStore((state) => state.users)
  const isLoadingUsers = useUserDirectoryStore((state) => state.isLoadingUsers)
  const userListError = useUserDirectoryStore((state) => state.userListError)
  const loadUsers = useUserDirectoryStore((state) => state.loadUsers)
  const reset = useUserDirectoryStore((state) => state.reset)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  useEffect(() => {
    if (!siteManagerEnabled) {
      reset()
      setNavigationIntent(null)
      return
    }

    let isCancelled = false
    setNavigationIntent(null)

    void loadUsers().then((result) => {
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
  }, [loadUsers, reset, siteManagerEnabled])

  return {
    users,
    isLoadingUsers,
    userListError,
    navigationIntent,
  }
}
