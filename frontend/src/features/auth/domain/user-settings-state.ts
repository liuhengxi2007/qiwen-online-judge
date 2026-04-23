import {
  displayNameValue,
  emailAddressValue,
  problemTitleDisplayModeValue,
  userDisplayModeValue,
  userLocaleValue,
  type SessionResponse,
} from '@/features/auth/domain/auth'
import type { UserDisplayMode } from '@/features/user/model/UserDisplayMode'
import type { UserLocale } from '@/features/user/model/UserLocale'
import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'

export type UserSettingsState = {
  editedUser: SessionResponse | null
  displayName: string
  email: string
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
  errorMessage: string
  successMessage: string
  isSubmitting: boolean
  navigationIntent: NavigationIntent | null
}

export type UserSettingsAction =
  | { type: 'target_changed'; editedUser: SessionResponse | null }
  | { type: 'query_synced'; user: SessionResponse }
  | { type: 'query_failed'; message: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_display_mode'; value: UserDisplayMode }
  | { type: 'set_locale'; value: UserLocale }
  | { type: 'set_problem_title_display_mode'; value: ProblemTitleDisplayMode }
  | { type: 'set_current_password'; value: string }
  | { type: 'set_new_password'; value: string }
  | { type: 'set_confirm_new_password'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; user: SessionResponse; message: string }
  | { type: 'submit_failed'; message: string }
  | { type: 'redirect_requested'; intent: NavigationIntent }

export const initialUserSettingsState: UserSettingsState = {
  editedUser: null,
  displayName: '',
  email: '',
  displayMode: 'display_name',
  locale: 'en',
  problemTitleDisplayMode: 'title',
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
  errorMessage: '',
  successMessage: '',
  isSubmitting: false,
  navigationIntent: null,
}

export function reduceUserSettingsState(
  state: UserSettingsState,
  action: UserSettingsAction,
): UserSettingsState {
  switch (action.type) {
    case 'target_changed':
      return {
        ...state,
        editedUser: action.editedUser,
        displayName: '',
        email: '',
        displayMode: action.editedUser ? userDisplayModeValue(action.editedUser.preferences.displayMode) : 'display_name',
        locale: action.editedUser ? userLocaleValue(action.editedUser.preferences.locale) : 'en',
        problemTitleDisplayMode: action.editedUser
          ? problemTitleDisplayModeValue(action.editedUser.preferences.problemTitleDisplayMode)
          : 'title',
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
        displayMode: userDisplayModeValue(action.user.preferences.displayMode),
        locale: userLocaleValue(action.user.preferences.locale),
        problemTitleDisplayMode: problemTitleDisplayModeValue(action.user.preferences.problemTitleDisplayMode),
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
    case 'set_display_mode':
      return { ...state, displayMode: action.value }
    case 'set_locale':
      return { ...state, locale: action.value }
    case 'set_problem_title_display_mode':
      return { ...state, problemTitleDisplayMode: action.value }
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
        displayMode: userDisplayModeValue(action.user.preferences.displayMode),
        locale: userLocaleValue(action.user.preferences.locale),
        problemTitleDisplayMode: problemTitleDisplayModeValue(action.user.preferences.problemTitleDisplayMode),
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
        successMessage: '',
      }
    case 'redirect_requested':
      return {
        ...state,
        navigationIntent: action.intent,
      }
  }
}
