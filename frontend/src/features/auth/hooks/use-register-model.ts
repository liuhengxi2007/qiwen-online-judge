import { useCallback, useReducer } from 'react'

import { parseEmailAddress, parsePlaintextPassword } from '@/features/auth/lib/auth-parsers'
import type { RegisterRequest } from '@/features/auth/http/request/RegisterRequest'
import { useRegisterMutation } from '@/features/auth/hooks/use-register-mutation'
import { parseDisplayName, parseUsername } from '@/features/user/lib/user-parsers'

type RegisterState = {
  username: string
  displayName: string
  email: string
  password: string
  confirmPassword: string
  errorMessage: string
}

type RegisterAction =
  | { type: 'set_username'; value: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_password'; value: string }
  | { type: 'set_confirm_password'; value: string }
  | { type: 'validation_failed'; message: string }
  | { type: 'validation_cleared' }

const initialState: RegisterState = {
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
  errorMessage: '',
}

function registerReducer(state: RegisterState, action: RegisterAction): RegisterState {
  switch (action.type) {
    case 'set_username':
      return { ...state, username: action.value }
    case 'set_display_name':
      return { ...state, displayName: action.value }
    case 'set_email':
      return { ...state, email: action.value }
    case 'set_password':
      return { ...state, password: action.value }
    case 'set_confirm_password':
      return { ...state, confirmPassword: action.value }
    case 'validation_failed':
      return { ...state, errorMessage: action.message }
    case 'validation_cleared':
      return { ...state, errorMessage: '' }
  }
}

export function useRegisterModel() {
  const [state, dispatch] = useReducer(registerReducer, initialState)
  const mutation = useRegisterMutation()

  const submit = useCallback(async () => {
    const usernameResult = parseUsername(state.username)
    if (!usernameResult.ok) {
      dispatch({ type: 'validation_failed', message: usernameResult.error })
      return
    }

    const displayNameResult = parseDisplayName(state.displayName)
    if (!displayNameResult.ok) {
      dispatch({ type: 'validation_failed', message: displayNameResult.error })
      return
    }

    const emailResult = parseEmailAddress(state.email)
    if (!emailResult.ok) {
      dispatch({ type: 'validation_failed', message: emailResult.error })
      return
    }

    const passwordResult = parsePlaintextPassword(state.password)
    if (!passwordResult.ok) {
      dispatch({ type: 'validation_failed', message: passwordResult.error })
      return
    }

    const confirmPasswordResult = parsePlaintextPassword(state.confirmPassword)
    if (!confirmPasswordResult.ok) {
      dispatch({ type: 'validation_failed', message: confirmPasswordResult.error })
      return
    }

    if (passwordResult.value !== confirmPasswordResult.value) {
      dispatch({ type: 'validation_failed', message: 'Passwords do not match.' })
      return
    }

    dispatch({ type: 'validation_cleared' })

    await mutation.submitRegister({
      username: usernameResult.value,
      displayName: displayNameResult.value,
      email: emailResult.value,
      password: passwordResult.value,
    } satisfies RegisterRequest)
  }, [mutation, state.confirmPassword, state.displayName, state.email, state.password, state.username])

  return {
    username: state.username,
    displayName: state.displayName,
    email: state.email,
    password: state.password,
    confirmPassword: state.confirmPassword,
    errorMessage: state.errorMessage || mutation.errorMessage,
    isSubmitting: mutation.isSubmitting,
    navigationIntent: mutation.navigationIntent,
    setUsername: (value: string) => dispatch({ type: 'set_username', value }),
    setDisplayName: (value: string) => dispatch({ type: 'set_display_name', value }),
    setEmail: (value: string) => dispatch({ type: 'set_email', value }),
    setPassword: (value: string) => dispatch({ type: 'set_password', value }),
    setConfirmPassword: (value: string) => dispatch({ type: 'set_confirm_password', value }),
    submit,
  }
}
