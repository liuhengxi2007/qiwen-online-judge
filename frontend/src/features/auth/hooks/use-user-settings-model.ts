import { useEffect, useReducer } from 'react'

import {
  parseUsername,
  type SessionResponse,
} from '@/features/auth/domain/auth'
import {
  initialUserSettingsState,
  reduceUserSettingsState,
} from '@/features/auth/domain/user-settings-state'
import { validateUserSettingsDraft } from '@/features/auth/domain/user-settings-form'
import { useUserSettingsQuery } from '@/features/auth/hooks/use-user-settings-query'
import { useUserSettingsMutation } from '@/features/auth/hooks/use-user-settings-mutation'
import {
  resolveUserSettingsRoutePolicy,
  toForbiddenRedirect,
} from '@/features/auth/lib/route-policy'
import { useI18n } from '@/shared/i18n/i18n'
import type { UserDisplayMode } from '@/features/auth/model/UserDisplayMode'
import type { UserLocale } from '@/features/auth/model/UserLocale'
import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'

type UseUserSettingsModelArgs = {
  viewer: SessionResponse
  routeUsername: string | undefined
  setViewer: (session: SessionResponse | null) => void
}

export function useUserSettingsModel({ viewer, routeUsername, setViewer }: UseUserSettingsModelArgs) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reduceUserSettingsState, initialUserSettingsState)
  const mutation = useUserSettingsMutation()

  const parsedRouteUsername = routeUsername ? parseUsername(routeUsername) : null
  const siteManagerViewer = viewer.siteManager
  const routePolicy = resolveUserSettingsRoutePolicy({
    viewerUsername: viewer.username,
    routeUsername: parsedRouteUsername?.ok ? parsedRouteUsername.value : null,
    hasRouteUsername: Boolean(routeUsername),
    siteManagerViewer,
  })
  const targetUsername = routePolicy.targetUsername
  const isEditingOwnSettings = routePolicy.isEditingOwnSettings
  const canManageTarget = routePolicy.canManageTarget
  const query = useUserSettingsQuery({
    canLoadTarget: canManageTarget,
    targetUsername,
  })
  const displayedUser = isEditingOwnSettings ? viewer : state.editedUser
  const submitSettings = mutation.submitSettings

  useEffect(() => {
    dispatch({
      type: 'target_changed',
      editedUser: isEditingOwnSettings ? viewer : null,
    })
  }, [isEditingOwnSettings, targetUsername, viewer])

  useEffect(() => {
    if (query.editedUser) {
      dispatch({ type: 'query_synced', user: query.editedUser })
      return
    }

    if (query.settingsLoadError) {
      dispatch({ type: 'query_failed', message: query.settingsLoadError })
    }
  }, [query.editedUser, query.settingsLoadError])

  async function submit() {
    if (!displayedUser) {
      dispatch({ type: 'submit_failed', message: t('userSettings.loadingSelected') })
      return
    }

    const validation = validateUserSettingsDraft(
      {
        displayName: state.displayName,
        email: state.email,
        displayMode: state.displayMode,
        locale: state.locale,
        problemTitleDisplayMode: state.problemTitleDisplayMode,
        currentPassword: state.currentPassword,
        newPassword: state.newPassword,
        confirmNewPassword: state.confirmNewPassword,
      },
      isEditingOwnSettings,
    )
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return
    }

    dispatch({ type: 'submit_started' })

    const result = await submitSettings(
      validation.submission.kind === 'own'
        ? {
            kind: 'own',
            targetUsername,
            request: validation.submission.request,
            setViewer,
          }
        : {
            kind: 'managed',
            targetUsername,
            request: validation.submission.request,
            setViewer,
          },
    )

      switch (result.kind) {
        case 'updated':
          query.replaceEditedUser(targetUsername, result.user)
          dispatch({
            type: 'submit_succeeded',
            user: result.user,
            message: result.message,
          })
          return
        case 'updated_and_signed_out':
          return
        case 'forbidden':
          dispatch({ type: 'redirect_requested', intent: toForbiddenRedirect() })
          return
        case 'unauthorized':
          dispatch({ type: 'submit_failed', message: result.message })
          return
        case 'failed':
          dispatch({ type: 'submit_failed', message: result.message })
          return
      }
  }

  return {
    ...state,
    displayedUser,
    isEditingOwnSettings,
    targetUsername,
    isSubmitting: state.isSubmitting || mutation.isSubmitting,
    isLoadingSettings: query.isLoadingSettings,
    navigationIntent: routePolicy.navigationIntent ?? state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent,
    setDisplayName: (value: string) => dispatch({ type: 'set_display_name', value }),
    setEmail: (value: string) => dispatch({ type: 'set_email', value }),
    setDisplayMode: (value: UserDisplayMode) => dispatch({ type: 'set_display_mode', value }),
    setLocale: (value: UserLocale) => dispatch({ type: 'set_locale', value }),
    setProblemTitleDisplayMode: (value: ProblemTitleDisplayMode) =>
      dispatch({ type: 'set_problem_title_display_mode', value }),
    setCurrentPassword: (value: string) => dispatch({ type: 'set_current_password', value }),
    setNewPassword: (value: string) => dispatch({ type: 'set_new_password', value }),
    setConfirmNewPassword: (value: string) => dispatch({ type: 'set_confirm_new_password', value }),
    submit,
  }
}
