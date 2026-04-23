import { useCallback, useState } from 'react'

import {
  toAuthSession,
  type SessionResponse,
  type Username,
} from '@/features/auth/domain/auth'
import { logout } from '@/features/auth/api/auth-client'
import {
  UserClientError,
  updateManagedUserSettings,
  updateOwnUserSettings,
} from '@/features/user/api/user-client'
import type { UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest } from '@/features/user/domain/user'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toPasswordChangedRedirect, toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { useI18n } from '@/shared/i18n/i18n'

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
  const { t } = useI18n()
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

        if (shouldSignOutAfterUpdate) {
          await logout()
          params.setViewer(null)
          setNavigationIntent(toPasswordChangedRedirect())
          setIsSubmitting(false)
          return { kind: 'updated_and_signed_out' }
        }

        const message = t('userSettings.updateSuccess')

        setIsSubmitting(false)
        return { kind: 'updated', user: updatedUser, message }
      } catch (error) {
        if (error instanceof UserClientError && error.kind === 'forbidden') {
          setIsSubmitting(false)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        if (error instanceof UserClientError && error.kind === 'unauthorized') {
          const message =
            error.message ||
            (params.kind === 'own' ? t('userSettings.currentPasswordTitle') : t('userSettings.updateFailed'))
          setIsSubmitting(false)
          return { kind: 'unauthorized', message }
        }

        const message = t('userSettings.updateFailed')
        setIsSubmitting(false)
        return { kind: 'failed', message }
      }
    },
    [t],
  )

  return {
    isSubmitting,
    navigationIntent,
    submitSettings,
  }
}
