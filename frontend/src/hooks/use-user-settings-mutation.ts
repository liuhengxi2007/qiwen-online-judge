import { useCallback, useState } from 'react'

import {
  toAuthSession,
  type SessionResponse,
  type UpdateManagedUserSettingsRequest,
  type UpdateOwnSettingsRequest,
} from '@/domain/auth'
import { AuthClientError, updateManagedUserSettings, updateOwnUserSettings } from '@/lib/auth-client'
import type { NavigationIntent } from '@/lib/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/lib/route-policy'
import { useUserSettingsQueryStore } from '@/stores/use-user-settings-query-store'

type SubmitSettingsParams =
  | {
      kind: 'own'
      targetUsername: string
      request: UpdateOwnSettingsRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'managed'
      targetUsername: string
      request: UpdateManagedUserSettingsRequest
      setViewer: (session: SessionResponse | null) => void
    }

type SubmitSettingsResult =
  | { kind: 'updated'; user: SessionResponse; message: string }
  | { kind: 'forbidden' }
  | { kind: 'unauthorized'; message: string }
  | { kind: 'failed'; message: string }

export function useUserSettingsMutation() {
  const setEditedUser = useUserSettingsQueryStore((state) => state.setEditedUser)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const submitSettings = useCallback(
    async (params: SubmitSettingsParams): Promise<SubmitSettingsResult> => {
      setIsSubmitting(true)
      setNavigationIntent(null)

      try {
        const updatedUser =
          params.kind === 'own'
            ? await updateOwnUserSettings(params.targetUsername, params.request)
            : await updateManagedUserSettings(params.targetUsername, params.request)

        if (params.kind === 'own') {
          params.setViewer(toAuthSession(updatedUser))
        }

        setEditedUser(params.targetUsername, updatedUser)
        const message =
          params.kind === 'own'
            ? 'Settings updated successfully.'
            : `Settings updated for ${updatedUser.username}.`

        setIsSubmitting(false)
        return { kind: 'updated', user: updatedUser, message }
      } catch (error) {
        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setIsSubmitting(false)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        if (error instanceof AuthClientError && error.kind === 'unauthorized') {
          const message =
            error.message ||
            (params.kind === 'own' ? 'Current password is incorrect.' : 'Unable to update settings.')
          setIsSubmitting(false)
          return { kind: 'unauthorized', message }
        }

        const message = 'Unable to update settings.'
        setIsSubmitting(false)
        return { kind: 'failed', message }
      }
    },
    [setEditedUser],
  )

  return {
    isSubmitting,
    navigationIntent,
    submitSettings,
  }
}
