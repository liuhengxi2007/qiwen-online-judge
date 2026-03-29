import { useCallback, useReducer } from 'react'

import { parsePlaintextPassword, parseUsername, type LoginRequest } from '@/domain/auth'
import { useLoginMutation } from '@/hooks/use-login-mutation'

type LoginState = {
  username: string
  password: string
  errorMessage: string
}

type LoginAction =
  | { type: 'set_username'; value: string }
  | { type: 'set_password'; value: string }
  | { type: 'validation_failed'; message: string }
  | { type: 'validation_cleared' }

const initialState: LoginState = {
  username: '',
  password: '',
  errorMessage: '',
}

function loginReducer(state: LoginState, action: LoginAction): LoginState {
  switch (action.type) {
    case 'set_username':
      return { ...state, username: action.value }
    case 'set_password':
      return { ...state, password: action.value }
    case 'validation_failed':
      return { ...state, errorMessage: action.message }
    case 'validation_cleared':
      return { ...state, errorMessage: '' }
  }
}

export function useLoginModel() {
  const [state, dispatch] = useReducer(loginReducer, initialState)
  const mutation = useLoginMutation()

  const submit = useCallback(async () => {
    const usernameResult = parseUsername(state.username)
    if (!usernameResult.ok) {
      dispatch({ type: 'validation_failed', message: usernameResult.error })
      return
    }

    const passwordResult = parsePlaintextPassword(state.password)
    if (!passwordResult.ok) {
      dispatch({ type: 'validation_failed', message: passwordResult.error })
      return
    }

    dispatch({ type: 'validation_cleared' })

    await mutation.submitLogin({
      username: usernameResult.value,
      password: passwordResult.value,
    } satisfies LoginRequest)
  }, [mutation, state.password, state.username])

  return {
    username: state.username,
    password: state.password,
    errorMessage: state.errorMessage || mutation.errorMessage,
    isSubmitting: mutation.isSubmitting,
    navigationIntent: mutation.navigationIntent,
    setUsername: (value: string) => dispatch({ type: 'set_username', value }),
    setPassword: (value: string) => dispatch({ type: 'set_password', value }),
    submit,
  }
}
