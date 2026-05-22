import { useCallback, useState } from 'react'

import {
  toAuthSession,
  type SessionResponse,
} from '@/features/auth/domain/auth'
import type { Username } from '@/features/user/domain/user'
import { logout } from '@/features/auth/http/api/auth-client'
import {
  UserClientError,
  updateManagedUserAccount,
  updateManagedUserPreferences,
  updateManagedUserProfile,
  updateOwnUserAccount,
  updateOwnUserPreferences,
  updateOwnUserProfile,
} from '@/features/user/http/api/user-client'
import type {
  UpdateManagedUserAccountRequest,
  UpdateManagedUserPreferencesRequest,
  UpdateManagedUserProfileRequest,
  UpdateOwnAccountRequest,
  UpdateOwnPreferencesRequest,
  UpdateOwnProfileRequest,
} from '@/features/user/domain/user'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toPasswordChangedRedirect, toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { useI18n } from '@/shared/i18n/use-i18n'

type SubmitSettingsParams =
  | {
      kind: 'own_profile'
      targetUsername: Username
      request: UpdateOwnProfileRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'own_preferences'
      targetUsername: Username
      request: UpdateOwnPreferencesRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'own_account'
      targetUsername: Username
      request: UpdateOwnAccountRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'managed_profile'
      targetUsername: Username
      request: UpdateManagedUserProfileRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'managed_preferences'
      targetUsername: Username
      request: UpdateManagedUserPreferencesRequest
      setViewer: (session: SessionResponse | null) => void
    }
  | {
      kind: 'managed_account'
      targetUsername: Username
      request: UpdateManagedUserAccountRequest
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
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const submitSettings = useCallback(
    async (params: SubmitSettingsParams): Promise<SubmitSettingsResult> => {
      setNavigationIntent(null)

      try {
        const updatedUser = await (() => {
          switch (params.kind) {
            case 'own_profile':
              return updateOwnUserProfile(params.targetUsername, params.request)
            case 'own_preferences':
              return updateOwnUserPreferences(params.targetUsername, params.request)
            case 'own_account':
              return updateOwnUserAccount(params.targetUsername, params.request)
            case 'managed_profile':
              return updateManagedUserProfile(params.targetUsername, params.request)
            case 'managed_preferences':
              return updateManagedUserPreferences(params.targetUsername, params.request)
            case 'managed_account':
              return updateManagedUserAccount(params.targetUsername, params.request)
          }
        })()

        const shouldSignOutAfterUpdate = params.kind === 'own_account' && params.request.newPassword !== null

        if ((params.kind === 'own_profile' || params.kind === 'own_preferences' || params.kind === 'own_account') && !shouldSignOutAfterUpdate) {
          params.setViewer(toAuthSession(updatedUser))
        }

        if (shouldSignOutAfterUpdate) {
          await logout()
          params.setViewer(null)
          setNavigationIntent(toPasswordChangedRedirect())
          return { kind: 'updated_and_signed_out' }
        }

        const message =
          params.kind === 'own_profile' || params.kind === 'managed_profile'
            ? t('userSettings.profileUpdateSuccess')
            : params.kind === 'own_preferences' || params.kind === 'managed_preferences'
              ? t('userSettings.preferencesUpdateSuccess')
              : t('userSettings.accountUpdateSuccess')

        return { kind: 'updated', user: updatedUser, message }
      } catch (error) {
        if (error instanceof UserClientError && error.kind === 'forbidden') {
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        if (error instanceof UserClientError && error.kind === 'unauthorized') {
          const message =
            error.message ||
            (params.kind === 'own_account' ? t('userSettings.currentPasswordTitle') : t('userSettings.updateFailed'))
          return { kind: 'unauthorized', message }
        }

        const message = t('userSettings.updateFailed')
        return { kind: 'failed', message }
      }
    },
    [t],
  )

  return {
    navigationIntent,
    submitSettings,
  }
}
