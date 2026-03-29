import { useCallback, useReducer } from 'react'
import { useNavigate } from 'react-router-dom'

import {
  parseDisplayName,
  parseEmailAddress,
  parsePlaintextPassword,
  parseUsername,
  toAuthSession,
  type RegisterRequest,
} from '@/domain/auth'
import { register } from '@/lib/auth-client'
import { useAuthStore } from '@/stores/use-auth-store'

type RegisterState = {
  username: string
  displayName: string
  email: string
  password: string
  confirmPassword: string
  errorMessage: string
  isSubmitting: boolean
}

type RegisterAction =
  | { type: 'set_username'; value: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_password'; value: string }
  | { type: 'set_confirm_password'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_failed'; message: string }
  | { type: 'submit_succeeded' }

const initialState: RegisterState = {
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
  errorMessage: '',
  isSubmitting: false,
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
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '' }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message }
    case 'submit_succeeded':
      return { ...state, isSubmitting: false, errorMessage: '' }
  }
}

export function useRegisterModel() {
  const navigate = useNavigate()
  const setSession = useAuthStore((state) => state.setSession)
  const [state, dispatch] = useReducer(registerReducer, initialState)

  const submit = useCallback(async () => {
    const usernameResult = parseUsername(state.username)
    if (!usernameResult.ok) {
      dispatch({ type: 'submit_failed', message: usernameResult.error })
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

    const passwordResult = parsePlaintextPassword(state.password)
    if (!passwordResult.ok) {
      dispatch({ type: 'submit_failed', message: passwordResult.error })
      return
    }

    const confirmPasswordResult = parsePlaintextPassword(state.confirmPassword)
    if (!confirmPasswordResult.ok) {
      dispatch({ type: 'submit_failed', message: confirmPasswordResult.error })
      return
    }

    if (passwordResult.value !== confirmPasswordResult.value) {
      dispatch({ type: 'submit_failed', message: 'Passwords do not match.' })
      return
    }

    dispatch({ type: 'submit_started' })

    try {
      const data = await register({
        username: usernameResult.value,
        displayName: displayNameResult.value,
        email: emailResult.value,
        password: passwordResult.value,
      } satisfies RegisterRequest)

      setSession(toAuthSession(data))
      dispatch({ type: 'submit_succeeded' })
      navigate('/')
    } catch (error) {
      dispatch({
        type: 'submit_failed',
        message:
          error instanceof Error
            ? error.message
            : 'Unable to reach the server. Please start the backend service.',
      })
    }
  }, [navigate, setSession, state.confirmPassword, state.displayName, state.email, state.password, state.username])

  return {
    ...state,
    setUsername: (value: string) => dispatch({ type: 'set_username', value }),
    setDisplayName: (value: string) => dispatch({ type: 'set_display_name', value }),
    setEmail: (value: string) => dispatch({ type: 'set_email', value }),
    setPassword: (value: string) => dispatch({ type: 'set_password', value }),
    setConfirmPassword: (value: string) => dispatch({ type: 'set_confirm_password', value }),
    submit,
  }
}
