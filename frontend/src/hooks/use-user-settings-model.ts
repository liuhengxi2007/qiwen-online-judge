import { useCallback, useEffect, useReducer } from 'react'
import { useNavigate } from 'react-router-dom'

import {
  displayNameValue,
  emailAddressValue,
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  toAuthSession,
  usernameValue,
  type SessionResponse,
  type UpdateManagedUserSettingsRequest,
  type UpdateOwnSettingsRequest,
} from '@/domain/auth'
import {
  AuthClientError,
  getUserSettings,
  updateManagedUserSettings,
  updateOwnUserSettings,
} from '@/lib/auth-client'

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
}

type UserSettingsAction =
  | { type: 'load_started'; editedUser: SessionResponse | null }
  | { type: 'load_succeeded'; user: SessionResponse }
  | { type: 'load_failed'; message: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_current_password'; value: string }
  | { type: 'set_new_password'; value: string }
  | { type: 'set_confirm_new_password'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; user: SessionResponse; message: string }
  | { type: 'submit_failed'; message: string }

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
}

function userSettingsReducer(state: UserSettingsState, action: UserSettingsAction): UserSettingsState {
  switch (action.type) {
    case 'load_started':
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
      }
    case 'load_succeeded':
      return {
        ...state,
        editedUser: action.user,
        displayName: displayNameValue(action.user.displayName),
        email: emailAddressValue(action.user.email),
        errorMessage: '',
      }
    case 'load_failed':
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
  }
}

type UseUserSettingsModelArgs = {
  viewer: SessionResponse
  routeUsername: string | undefined
  setViewer: (session: SessionResponse | null) => void
}

export function useUserSettingsModel({ viewer, routeUsername, setViewer }: UseUserSettingsModelArgs) {
  const navigate = useNavigate()
  const [state, dispatch] = useReducer(userSettingsReducer, initialState)

  const viewerUsername = usernameValue(viewer.username)
  const siteManagerViewer = viewer.siteManager
  const targetUsername = routeUsername?.trim() || viewerUsername
  const isEditingOwnSettings = targetUsername.toLowerCase() === viewerUsername.toLowerCase()
  const canManageTarget = isEditingOwnSettings || siteManagerViewer
  const displayedUser = isEditingOwnSettings ? viewer : state.editedUser

  useEffect(() => {
    if (!routeUsername && !siteManagerViewer) {
      navigate(`/user/${viewerUsername}/settings?notice=route-corrected`, { replace: true })
      return
    }

    if (routeUsername && !siteManagerViewer && routeUsername.toLowerCase() !== viewerUsername.toLowerCase()) {
      navigate(`/user/${viewerUsername}/settings?notice=route-corrected`, { replace: true })
      return
    }

    if (routeUsername && !canManageTarget) {
      navigate('/?notice=site-manage-denied', { replace: true })
    }
  }, [canManageTarget, navigate, routeUsername, siteManagerViewer, viewerUsername])

  useEffect(() => {
    let isCancelled = false

    const load = async () => {
      if (!canManageTarget) {
        return
      }

      dispatch({
        type: 'load_started',
        editedUser: isEditingOwnSettings ? viewer : null,
      })

      try {
        const user = await getUserSettings(targetUsername)

        if (!isCancelled) {
          dispatch({ type: 'load_succeeded', user })
        }
      } catch (error) {
        if (isCancelled) {
          return
        }

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          navigate('/?notice=site-manage-denied', { replace: true })
          return
        }

        if (error instanceof AuthClientError && error.kind === 'not-found') {
          dispatch({ type: 'load_failed', message: 'User not found.' })
          return
        }

        dispatch({ type: 'load_failed', message: 'Unable to load settings.' })
      }
    }

    void load()

    return () => {
      isCancelled = true
    }
  }, [canManageTarget, isEditingOwnSettings, navigate, targetUsername, viewer])

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

    try {
      const updatedUser = isEditingOwnSettings
        ? await updateOwnUserSettings(
            targetUsername,
            {
              displayName: displayNameResult.value,
              email: emailResult.value,
              currentPassword: currentPasswordValue!,
              newPassword: newPasswordResult ? newPasswordResult.value : null,
            } satisfies UpdateOwnSettingsRequest,
          )
        : await updateManagedUserSettings(
            targetUsername,
            {
              displayName: displayNameResult.value,
              email: emailResult.value,
              newPassword: newPasswordResult ? newPasswordResult.value : null,
            } satisfies UpdateManagedUserSettingsRequest,
          )

      if (isEditingOwnSettings) {
        setViewer(toAuthSession(updatedUser))
      }

      dispatch({
        type: 'submit_succeeded',
        user: updatedUser,
        message: isEditingOwnSettings
          ? 'Settings updated successfully.'
          : `Settings updated for ${usernameValue(updatedUser.username)}.`,
      })
    } catch (error) {
      if (error instanceof AuthClientError && error.kind === 'forbidden') {
        navigate('/?notice=site-manage-denied', { replace: true })
        return
      }

      if (error instanceof AuthClientError && error.kind === 'unauthorized') {
        dispatch({
          type: 'submit_failed',
          message: error.message || (isEditingOwnSettings ? 'Current password is incorrect.' : 'Unable to update settings.'),
        })
        return
      }

      dispatch({ type: 'submit_failed', message: 'Unable to update settings.' })
    }
  }, [displayedUser, isEditingOwnSettings, navigate, setViewer, state, targetUsername])

  return {
    ...state,
    displayedUser,
    isEditingOwnSettings,
    targetUsername,
    setDisplayName: (value: string) => dispatch({ type: 'set_display_name', value }),
    setEmail: (value: string) => dispatch({ type: 'set_email', value }),
    setCurrentPassword: (value: string) => dispatch({ type: 'set_current_password', value }),
    setNewPassword: (value: string) => dispatch({ type: 'set_new_password', value }),
    setConfirmNewPassword: (value: string) => dispatch({ type: 'set_confirm_new_password', value }),
    submit,
  }
}
