import { useCallback, useReducer } from 'react'

import { parseEmailAddress } from '@/objects/auth/EmailAddress'
import { parsePlaintextPassword } from '@/objects/auth/PlaintextPassword'
import type { RegisterRequest } from '@/objects/auth/request/RegisterRequest'
import { useRegisterMutation } from './useRegisterMutation'
import { parseDisplayName } from '@/objects/user/DisplayName'
import { parseUsername } from '@/objects/user/Username'

/**
 * 注册表单状态，保存提交中标记和错误消息。
 */
type RegisterState = {
  draft: RegisterDraft
  errorMessage: string
}

/**
 * 注册表单草稿，保存用户名、显示名、邮箱和密码输入。
 */
type RegisterDraft = {
  username: string
  displayName: string
  email: string
  password: string
  confirmPassword: string
}

/**
 * 注册表单 reducer 动作，覆盖字段编辑和提交状态。
 */
type RegisterAction =
  | { type: 'set_username'; value: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_password'; value: string }
  | { type: 'set_confirm_password'; value: string }
  | { type: 'validation_failed'; message: string }
  | { type: 'validation_cleared' }

const initialDraft: RegisterDraft = {
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
}

const initialState: RegisterState = {
  draft: initialDraft,
  errorMessage: '',
}

/**
 * 注册表单 reducer；纯函数维护草稿、提交标记和错误文案。
 */
function registerReducer(state: RegisterState, action: RegisterAction): RegisterState {
  switch (action.type) {
    case 'set_username':
      return { ...state, draft: { ...state.draft, username: action.value } }
    case 'set_display_name':
      return { ...state, draft: { ...state.draft, displayName: action.value } }
    case 'set_email':
      return { ...state, draft: { ...state.draft, email: action.value } }
    case 'set_password':
      return { ...state, draft: { ...state.draft, password: action.value } }
    case 'set_confirm_password':
      return { ...state, draft: { ...state.draft, confirmPassword: action.value } }
    case 'validation_failed':
      return { ...state, errorMessage: action.message }
    case 'validation_cleared':
      return { ...state, errorMessage: '' }
  }
}

/**
 * 注册表单模型 hook；维护注册草稿并提供提交状态更新动作。
 */
export function useRegisterModel() {
  const [state, dispatch] = useReducer(registerReducer, initialState)
  const mutation = useRegisterMutation()

  const submit = useCallback(async () => {
    const usernameResult = parseUsername(state.draft.username)
    if (!usernameResult.ok) {
      dispatch({ type: 'validation_failed', message: usernameResult.error })
      return
    }

    const displayNameResult = parseDisplayName(state.draft.displayName)
    if (!displayNameResult.ok) {
      dispatch({ type: 'validation_failed', message: displayNameResult.error })
      return
    }

    const emailResult = parseEmailAddress(state.draft.email)
    if (!emailResult.ok) {
      dispatch({ type: 'validation_failed', message: emailResult.error })
      return
    }

    const passwordResult = parsePlaintextPassword(state.draft.password)
    if (!passwordResult.ok) {
      dispatch({ type: 'validation_failed', message: passwordResult.error })
      return
    }

    const confirmPasswordResult = parsePlaintextPassword(state.draft.confirmPassword)
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
  }, [mutation, state.draft])

  return {
    username: state.draft.username,
    displayName: state.draft.displayName,
    email: state.draft.email,
    password: state.draft.password,
    confirmPassword: state.draft.confirmPassword,
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
