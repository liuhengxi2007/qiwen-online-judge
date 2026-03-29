import { useCallback, useReducer } from 'react'
import { useNavigate } from 'react-router-dom'

import { parsePlaintextPassword, parseUsername, toAuthSession, type LoginRequest } from '@/domain/auth'
import { login } from '@/lib/auth-client'
import { useAuthStore } from '@/stores/use-auth-store'

type LoginState = {
  username: string
  password: string
  errorMessage: string
  isSubmitting: boolean
}

type LoginAction =
  | { type: 'set_username'; value: string }
  | { type: 'set_password'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_failed'; message: string }
  | { type: 'submit_succeeded' }

const initialState: LoginState = {
  username: 'admin',
  password: 'password123',
  errorMessage: '',
  isSubmitting: false,
}

function loginReducer(state: LoginState, action: LoginAction): LoginState {
  switch (action.type) {
    case 'set_username':
      return { ...state, username: action.value }
    case 'set_password':
      return { ...state, password: action.value }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '' }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message }
    case 'submit_succeeded':
      return { ...state, isSubmitting: false, errorMessage: '' }
  }
}

export function useLoginModel() {
  const navigate = useNavigate()
  const setSession = useAuthStore((state) => state.setSession)
  const [state, dispatch] = useReducer(loginReducer, initialState)

  const submit = useCallback(async () => {
    const usernameResult = parseUsername(state.username)
    if (!usernameResult.ok) {
      dispatch({ type: 'submit_failed', message: usernameResult.error })
      return
    }

    const passwordResult = parsePlaintextPassword(state.password)
    if (!passwordResult.ok) {
      dispatch({ type: 'submit_failed', message: passwordResult.error })
      return
    }

    dispatch({ type: 'submit_started' })

    try {
      const data = await login({
        username: usernameResult.value,
        password: passwordResult.value,
      } satisfies LoginRequest)

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
  }, [navigate, setSession, state.password, state.username])

  return {
    ...state,
    setUsername: (value: string) => dispatch({ type: 'set_username', value }),
    setPassword: (value: string) => dispatch({ type: 'set_password', value }),
    submit,
  }
}
