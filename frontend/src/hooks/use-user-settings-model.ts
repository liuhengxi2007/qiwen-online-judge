import { useCallback, useEffect, useReducer } from 'react'

import {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  usernameValue,
  type SessionResponse,
  type UpdateManagedUserSettingsRequest,
  type UpdateOwnSettingsRequest,
} from '@/domain/auth'
import { useUserSettingsQuery } from '@/hooks/use-user-settings-query'
import { useUserSettingsMutation } from '@/hooks/use-user-settings-mutation'
import type { NavigationIntent } from '@/lib/navigation-intent'
import {
  resolveUserSettingsRoutePolicy,
  toSiteManageDeniedRedirect,
} from '@/lib/route-policy'

type UserSettingsState = {
  editedUser: SessionResponse | null
  displayName: string
  email: string
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
  errorMessage: string
  successMessage: string
  isSubmitting: boolean
  navigationIntent: NavigationIntent | null
}

type UserSettingsAction =
  | { type: 'target_changed'; editedUser: SessionResponse | null }
  | { type: 'query_synced'; user: SessionResponse }
  | { type: 'query_failed'; message: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_current_password'; value: string }
  | { type: 'set_new_password'; value: string }
  | { type: 'set_confirm_new_password'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; user: SessionResponse; message: string }
  | { type: 'submit_failed'; message: string }
  | { type: 'redirect_requested'; intent: NavigationIntent }

const initialState: UserSettingsState = {
  editedUser: null,
  displayName: '',
  email: '',
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
  errorMessage: '',
  successMessage: '',
  isSubmitting: false,
  navigationIntent: null,
}

function userSettingsReducer(state: UserSettingsState, action: UserSettingsAction): UserSettingsState {
  switch (action.type) {
    case 'target_changed':
      return {
        ...state,
        editedUser: action.editedUser,
        displayName: '',
        email: '',
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: '',
        errorMessage: '',
        successMessage: '',
        isSubmitting: false,
        navigationIntent: null,
      }
    case 'query_synced':
      return {
        ...state,
        editedUser: action.user,
        displayName: displayNameValue(action.user.displayName),
        email: emailAddressValue(action.user.email),
        errorMessage: '',
      }
    case 'query_failed':
      return {
        ...state,
        errorMessage: action.message,
        successMessage: '',
      }
    case 'set_display_name':
      return { ...state, displayName: action.value }
    case 'set_email':
      return { ...state, email: action.value }
    case 'set_current_password':
      return { ...state, currentPassword: action.value }
    case 'set_new_password':
      return { ...state, newPassword: action.value }
    case 'set_confirm_new_password':
      return { ...state, confirmNewPassword: action.value }
    case 'submit_started':
      return {
        ...state,
        isSubmitting: true,
        errorMessage: '',
        successMessage: '',
      }
    case 'submit_succeeded':
      return {
        ...state,
        editedUser: action.user,
        displayName: displayNameValue(action.user.displayName),
        email: emailAddressValue(action.user.email),
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: '',
        isSubmitting: false,
        errorMessage: '',
        successMessage: action.message,
      }
    case 'submit_failed':
      return {
        ...state,
        isSubmitting: false,
        errorMessage: action.message,
      }
    case 'redirect_requested':
      return {
        ...state,
        navigationIntent: action.intent,
      }
  }
}

type UseUserSettingsModelArgs = {
  viewer: SessionResponse
  routeUsername: string | undefined
  setViewer: (session: SessionResponse | null) => void
}

export function useUserSettingsModel({ viewer, routeUsername, setViewer }: UseUserSettingsModelArgs) {
  const [state, dispatch] = useReducer(userSettingsReducer, initialState)
  const mutation = useUserSettingsMutation()

  const viewerUsername = usernameValue(viewer.username)
  const siteManagerViewer = viewer.siteManager
  const routePolicy = resolveUserSettingsRoutePolicy({
    viewerUsername,
    routeUsername,
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

  useEffect(() => {
    if (routePolicy.navigationIntent) {
      dispatch({
        type: 'redirect_requested',
        intent: routePolicy.navigationIntent,
      })
    }
  }, [routePolicy.navigationIntent])

  useEffect(() => {
    dispatch({
      type: 'target_changed',
      editedUser: isEditingOwnSettings ? viewer : null,
    })
  }, [isEditingOwnSettings, targetUsername])

  useEffect(() => {
    if (query.editedUser) {
      dispatch({ type: 'query_synced', user: query.editedUser })
      return
    }

    if (query.settingsLoadError) {
      dispatch({ type: 'query_failed', message: query.settingsLoadError })
    }
  }, [query.editedUser, query.settingsLoadError])

  const submit = useCallback(async () => {
    if (!displayedUser) {
      dispatch({ type: 'submit_failed', message: 'Unable to load settings.' })
      return
    }

    const displayNameResult = parseDisplayName(state.displayName)
    if (!displayNameResult.ok) {
      dispatch({ type: 'submit_failed', message: displayNameResult.error })
      return
    }

    const emailResult = parseEmailAddress(state.email)
    if (!emailResult.ok) {
      dispatch({ type: 'submit_failed', message: emailResult.error })
      return
    }

    const currentPasswordResult =
      isEditingOwnSettings || state.currentPassword.trim()
        ? parsePlaintextPassword(state.currentPassword)
        : null

    if (isEditingOwnSettings && (!currentPasswordResult || !currentPasswordResult.ok)) {
      dispatch({ type: 'submit_failed', message: currentPasswordResult?.error ?? 'Password is required.' })
      return
    }

    const newPasswordResult = state.newPassword.trim() ? parsePlaintextPassword(state.newPassword) : null
    if (newPasswordResult && !newPasswordResult.ok) {
      dispatch({ type: 'submit_failed', message: newPasswordResult.error })
      return
    }

    const confirmNewPasswordResult = state.confirmNewPassword.trim()
      ? parsePlaintextPassword(state.confirmNewPassword)
      : null
    if (confirmNewPasswordResult && !confirmNewPasswordResult.ok) {
      dispatch({ type: 'submit_failed', message: confirmNewPasswordResult.error })
      return
    }

    if (newPasswordResult || confirmNewPasswordResult) {
      if (
        !newPasswordResult ||
        !confirmNewPasswordResult ||
        newPasswordResult.value !== confirmNewPasswordResult.value
      ) {
        dispatch({ type: 'submit_failed', message: 'New passwords do not match.' })
        return
      }
    }

    const currentPasswordValue =
      currentPasswordResult && currentPasswordResult.ok ? currentPasswordResult.value : null

    dispatch({ type: 'submit_started' })

    const result = await mutation.submitSettings(
      isEditingOwnSettings
        ? {
            kind: 'own',
            targetUsername,
            request: {
              displayName: displayNameResult.value,
              email: emailResult.value,
              currentPassword: currentPasswordValue!,
              newPassword: newPasswordResult ? newPasswordResult.value : null,
            } satisfies UpdateOwnSettingsRequest,
            setViewer,
          }
        : {
            kind: 'managed',
            targetUsername,
            request: {
              displayName: displayNameResult.value,
              email: emailResult.value,
              newPassword: newPasswordResult ? newPasswordResult.value : null,
            } satisfies UpdateManagedUserSettingsRequest,
            setViewer,
          },
    )

      switch (result.kind) {
        case 'updated':
        dispatch({
          type: 'submit_succeeded',
          user: result.user,
          message: result.message,
        })
        return
        case 'forbidden':
        dispatch({ type: 'redirect_requested', intent: toSiteManageDeniedRedirect() })
        return
      case 'unauthorized':
        dispatch({ type: 'submit_failed', message: result.message })
        return
      case 'failed':
        dispatch({ type: 'submit_failed', message: result.message })
        return
    }
  }, [displayedUser, isEditingOwnSettings, mutation, setViewer, state, targetUsername])

  return {
    ...state,
    displayedUser,
    isEditingOwnSettings,
    targetUsername,
    isSubmitting: state.isSubmitting || mutation.isSubmitting,
    isLoadingSettings: query.isLoadingSettings,
    navigationIntent: state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent,
    setDisplayName: (value: string) => dispatch({ type: 'set_display_name', value }),
    setEmail: (value: string) => dispatch({ type: 'set_email', value }),
    setCurrentPassword: (value: string) => dispatch({ type: 'set_current_password', value }),
    setNewPassword: (value: string) => dispatch({ type: 'set_new_password', value }),
    setConfirmNewPassword: (value: string) => dispatch({ type: 'set_confirm_new_password', value }),
    submit,
  }
}
