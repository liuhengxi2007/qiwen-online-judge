import { useCallback, useState } from 'react'

import { toAuthSession } from '@/objects/auth/auth-session'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { Username } from '@/objects/user/Username'
import { logout } from '@/apis/auth/Logout'
import { HttpClientError } from '@/system/api/http-client'
import { updateManagedAccount, updateOwnAccount } from '@/apis/auth/UpdateAccount'
import { updateManagedUserPreferences, updateOwnUserPreferences } from '@/apis/user/UpdateUserPreferences'
import { updateManagedUserProfile, updateOwnUserProfile } from '@/apis/user/UpdateUserProfile'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import type { NavigationIntent } from '@/pages/routing/navigation-intent'
import { toPasswordChangedRedirect, toSiteManageDeniedRedirect } from '@/pages/routing/route-policy'
import { useI18n } from '@/system/i18n/use-i18n'

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
              return updateOwnAccount(params.targetUsername, params.request)
            case 'managed_profile':
              return updateManagedUserProfile(params.targetUsername, params.request)
            case 'managed_preferences':
              return updateManagedUserPreferences(params.targetUsername, params.request)
            case 'managed_account':
              return updateManagedAccount(params.targetUsername, params.request)
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
        if (error instanceof HttpClientError && error.kind === 'forbidden') {
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        if (error instanceof HttpClientError && error.kind === 'unauthorized') {
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
