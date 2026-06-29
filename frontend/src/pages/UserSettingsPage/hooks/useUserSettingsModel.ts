import { useEffect, useReducer } from 'react'

import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { parseUsername } from '@/objects/user/Username'
import {
  initialUserSettingsState,
  reduceUserSettingsState,
  type UserSettingsSection,
} from '../functions/UserSettingsState'
import { validateUserAccountDraft } from '../functions/UserAccountForm'
import { validateUserPreferencesDraft, validateUserProfileDraft } from '../functions/UserSettingsForm'
import { useUserSettingsQuery } from './useUserSettingsQuery'
import { useUserSettingsMutation } from './useUserSettingsMutation'
import {
  resolveUserSettingsRoutePolicy,
  toForbiddenRedirect,
} from '@/pages/routing/RoutePolicy'
import { useI18n } from '@/system/i18n/use-i18n'
import { toAuthSession } from '@/pages/stores/auth/AuthSession'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'

/**
 * 用户设置模型 hook 输入，包含当前查看者、路由目标用户名和会话更新回调。
 */
type UseUserSettingsModelArgs = {
  viewer: SessionResponse
  routeUsername: string | undefined
  setViewer: (session: SessionResponse | null) => void
}

/**
 * 用户设置页模型 hook；维护资料/偏好/账户草稿、权限策略和提交动作。
 */
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
      targetUsername,
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

  async function submit(section: UserSettingsSection) {
    if (!displayedUser) {
      dispatch({ type: 'submit_failed', section, message: t('userSettings.loadingSelected') })
      return
    }

    const result =
      section === 'profile'
        ? await submitProfileSettings()
        : section === 'preferences'
          ? await submitPreferencesSettings()
          : await submitAccountSettings()

    switch (result.kind) {
      case 'updated':
        query.replaceEditedUser(targetUsername, result.user)
        dispatch({
          type: 'submit_succeeded',
          section,
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
        dispatch({ type: 'submit_failed', section, message: result.message })
        return
      case 'failed':
        dispatch({ type: 'submit_failed', section, message: result.message })
        return
    }
  }

  async function submitProfileSettings() {
    const validation = validateUserProfileDraft({ displayName: state.displayName })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', section: 'profile', message: validation.message })
      return { kind: 'failed', message: validation.message } as const
    }

    dispatch({ type: 'submit_started', section: 'profile' })
    // 注意：validateUserProfileDraft 已按当前表单构造请求；这里的断言只是在 own/managed 联合分支间收窄。
    return submitSettings(
      isEditingOwnSettings
        ? {
            kind: 'own_profile',
            targetUsername,
            request: validation.request as UpdateOwnProfileRequest,
            setViewer,
          }
        : {
            kind: 'managed_profile',
            targetUsername,
            request: validation.request as UpdateManagedUserProfileRequest,
            setViewer,
          },
    )
  }

  async function submitPreferencesSettings() {
    const validation = validateUserPreferencesDraft({
      displayMode: state.displayMode,
      locale: state.locale,
      problemTitleDisplayMode: state.problemTitleDisplayMode,
      autoMarkMessageRead: state.autoMarkMessageRead,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', section: 'preferences', message: validation.message })
      return { kind: 'failed', message: validation.message } as const
    }

    dispatch({ type: 'submit_started', section: 'preferences' })
    // 注意：validateUserPreferencesDraft 已按当前表单构造请求；这里的断言只是在 own/managed 联合分支间收窄。
    return submitSettings(
      isEditingOwnSettings
        ? {
            kind: 'own_preferences',
            targetUsername,
            request: validation.request as UpdateOwnPreferencesRequest,
            setViewer,
          }
        : {
            kind: 'managed_preferences',
            targetUsername,
            request: validation.request as UpdateManagedUserPreferencesRequest,
            setViewer,
          },
    )
  }

  async function submitAccountSettings() {
    const validation = validateUserAccountDraft(
      {
        email: state.email,
        currentPassword: state.currentPassword,
        newPassword: state.newPassword,
        confirmNewPassword: state.confirmNewPassword,
      },
      isEditingOwnSettings,
      {
        confirmNewPassword: t('userSettings.confirmNewPassword'),
        passwordMismatch: t('userSettings.passwordMismatch'),
      },
    )
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', section: 'account', message: validation.message })
      return { kind: 'failed', message: validation.message } as const
    }

    dispatch({ type: 'submit_started', section: 'account' })
    // 注意：validateUserAccountDraft 已按当前表单构造请求；这里的断言只是在 own/managed 联合分支间收窄。
    return submitSettings(
      isEditingOwnSettings
        ? {
            kind: 'own_account',
            targetUsername,
            request: validation.request as UpdateOwnAccountRequest,
            setViewer,
          }
        : {
            kind: 'managed_account',
            targetUsername,
            request: validation.request as UpdateManagedUserAccountRequest,
            setViewer,
          },
    )
  }

  return {
    ...state,
    displayedUser,
    isEditingOwnSettings,
    targetUsername,
    loadErrorMessage: state.loadErrorMessage,
    sections: state.sections,
    isLoadingSettings: query.isLoadingSettings,
    navigationIntent: routePolicy.navigationIntent ?? state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent,
    setDisplayName: (value: string) => dispatch({ type: 'set_display_name', value }),
    setEmail: (value: string) => dispatch({ type: 'set_email', value }),
    setDisplayMode: (value: UserDisplayMode) => dispatch({ type: 'set_display_mode', value }),
    setLocale: (value: UserLocale) => dispatch({ type: 'set_locale', value }),
    setProblemTitleDisplayMode: (value: ProblemTitleDisplayMode) =>
      dispatch({ type: 'set_problem_title_display_mode', value }),
    setAutoMarkMessageRead: (value: boolean) => dispatch({ type: 'set_auto_mark_message_read', value }),
    setCurrentPassword: (value: string) => dispatch({ type: 'set_current_password', value }),
    setNewPassword: (value: string) => dispatch({ type: 'set_new_password', value }),
    setConfirmNewPassword: (value: string) => dispatch({ type: 'set_confirm_new_password', value }),
    replaceDisplayedUser: (nextUser: SessionResponse) => {
      query.replaceEditedUser(targetUsername, nextUser)
      dispatch({ type: 'query_synced', user: nextUser })
      if (isEditingOwnSettings) {
        setViewer(toAuthSession(nextUser))
      }
    },
    submit,
  }
}
