import { useCallback, useState } from 'react'

import {
  toAuthSession,
  usernameValue,
  type SessionResponse,
  type UpdateManagedUserSettingsRequest,
  type UpdateOwnSettingsRequest,
  type Username,
} from '@/features/auth/domain/auth'
import {
  AuthClientError,
  logout,
  updateManagedUserSettings,
  updateOwnUserSettings,
} from '@/features/auth/api/auth-client'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toPasswordChangedRedirect, toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { useUserSettingsQueryStore } from '@/features/auth/stores/use-user-settings-query-store'

type SubmitSettingsParams =
  | {
      kind: 'own'
      targetUsername: Username
      request: UpdateOwnSettingsRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'managed'
      targetUsername: Username
      request: UpdateManagedUserSettingsRequest
      setViewer: (session: SessionResponse | null) => void
    }

type SubmitSettingsResult =
  | { kind: 'updated'; user: SessionResponse; message: string }
  | { kind: 'updated_and_signed_out' }
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

        const shouldSignOutAfterUpdate = params.kind === 'own' && params.request.newPassword !== null

        if (params.kind === 'own' && !shouldSignOutAfterUpdate) {
          params.setViewer(toAuthSession(updatedUser))
        }

        setEditedUser(params.targetUsername, updatedUser)

        if (shouldSignOutAfterUpdate) {
          await logout()
          params.setViewer(null)
          setNavigationIntent(toPasswordChangedRedirect())
          setIsSubmitting(false)
          return { kind: 'updated_and_signed_out' }
        }

        const message =
          params.kind === 'own'
            ? 'Settings updated successfully.'
            : `Settings updated for ${usernameValue(updatedUser.username)}.`

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
