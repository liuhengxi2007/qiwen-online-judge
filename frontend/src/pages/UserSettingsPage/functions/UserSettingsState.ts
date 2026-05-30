import { problemTitleDisplayModeValue } from '@/objects/problem/ProblemTitleDisplayMode'
import { displayNameValue } from '@/objects/user/DisplayName'
import { userDisplayModeValue } from '@/objects/user/UserDisplayMode'
import { userLocaleValue } from '@/objects/user/UserLocale'
import { emailAddressValue } from '@/objects/auth/EmailAddress'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import type { UserLocale } from '@/objects/user/UserLocale'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import type { Username } from '@/objects/user/Username'

export type UserSettingsSection = 'profile' | 'preferences' | 'account'

export type UserSettingsSectionState = {
  errorMessage: string
  successMessage: string
  isSubmitting: boolean
}

export type UserSettingsState = {
  targetUsername: Username | null
  editedUser: SessionResponse | null
  displayName: string
  email: string
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
  autoMarkMessageRead: boolean
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
  loadErrorMessage: string
  sections: Record<UserSettingsSection, UserSettingsSectionState>
  navigationIntent: NavigationIntent | null
}

export type UserSettingsAction =
  | { type: 'target_changed'; targetUsername: Username; editedUser: SessionResponse | null }
  | { type: 'query_synced'; user: SessionResponse }
  | { type: 'query_failed'; message: string }
  | { type: 'set_display_name'; value: string }
  | { type: 'set_email'; value: string }
  | { type: 'set_display_mode'; value: UserDisplayMode }
  | { type: 'set_locale'; value: UserLocale }
  | { type: 'set_problem_title_display_mode'; value: ProblemTitleDisplayMode }
  | { type: 'set_auto_mark_message_read'; value: boolean }
  | { type: 'set_current_password'; value: string }
  | { type: 'set_new_password'; value: string }
  | { type: 'set_confirm_new_password'; value: string }
  | { type: 'submit_started'; section: UserSettingsSection }
  | { type: 'submit_succeeded'; section: UserSettingsSection; user: SessionResponse; message: string }
  | { type: 'submit_failed'; section: UserSettingsSection; message: string }
  | { type: 'redirect_requested'; intent: NavigationIntent }

function initialSectionState(): UserSettingsSectionState {
  return {
    errorMessage: '',
    successMessage: '',
    isSubmitting: false,
  }
}

function initialSections(): Record<UserSettingsSection, UserSettingsSectionState> {
  return {
    profile: initialSectionState(),
    preferences: initialSectionState(),
    account: initialSectionState(),
  }
}

function clearSectionFeedback(
  sections: Record<UserSettingsSection, UserSettingsSectionState>,
  section: UserSettingsSection,
): Record<UserSettingsSection, UserSettingsSectionState> {
  return {
    ...sections,
    [section]: {
      ...sections[section],
      errorMessage: '',
      successMessage: '',
    },
  }
}

export const initialUserSettingsState: UserSettingsState = {
  targetUsername: null,
  editedUser: null,
  displayName: '',
  email: '',
  displayMode: 'display_name',
  locale: 'en',
  problemTitleDisplayMode: 'title',
  autoMarkMessageRead: false,
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
  loadErrorMessage: '',
  sections: initialSections(),
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
        targetUsername: action.targetUsername,
        editedUser: action.editedUser,
        displayName: '',
        email: '',
        displayMode: action.editedUser ? userDisplayModeValue(action.editedUser.preferences.displayMode) : 'display_name',
        locale: action.editedUser ? userLocaleValue(action.editedUser.preferences.locale) : 'en',
        problemTitleDisplayMode: action.editedUser
          ? problemTitleDisplayModeValue(action.editedUser.preferences.problemTitleDisplayMode)
          : 'title',
        autoMarkMessageRead: action.editedUser ? action.editedUser.preferences.autoMarkMessageRead : false,
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: '',
        loadErrorMessage: '',
        sections: initialSections(),
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
        autoMarkMessageRead: action.user.preferences.autoMarkMessageRead,
        loadErrorMessage: '',
      }
    case 'query_failed':
      return {
        ...state,
        loadErrorMessage: action.message,
      }
    case 'set_display_name':
      return {
        ...state,
        displayName: action.value,
        sections: clearSectionFeedback(state.sections, 'profile'),
      }
    case 'set_email':
      return {
        ...state,
        email: action.value,
        sections: clearSectionFeedback(state.sections, 'account'),
      }
    case 'set_display_mode':
      return {
        ...state,
        displayMode: action.value,
        sections: clearSectionFeedback(state.sections, 'preferences'),
      }
    case 'set_locale':
      return {
        ...state,
        locale: action.value,
        sections: clearSectionFeedback(state.sections, 'preferences'),
      }
    case 'set_problem_title_display_mode':
      return {
        ...state,
        problemTitleDisplayMode: action.value,
        sections: clearSectionFeedback(state.sections, 'preferences'),
      }
    case 'set_auto_mark_message_read':
      return {
        ...state,
        autoMarkMessageRead: action.value,
        sections: clearSectionFeedback(state.sections, 'preferences'),
      }
    case 'set_current_password':
      return {
        ...state,
        currentPassword: action.value,
        sections: clearSectionFeedback(state.sections, 'account'),
      }
    case 'set_new_password':
      return {
        ...state,
        newPassword: action.value,
        sections: clearSectionFeedback(state.sections, 'account'),
      }
    case 'set_confirm_new_password':
      return {
        ...state,
        confirmNewPassword: action.value,
        sections: clearSectionFeedback(state.sections, 'account'),
      }
    case 'submit_started':
      return {
        ...state,
        sections: {
          ...state.sections,
          [action.section]: {
            errorMessage: '',
            successMessage: '',
            isSubmitting: true,
          },
        },
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
        autoMarkMessageRead: action.user.preferences.autoMarkMessageRead,
        currentPassword: action.section === 'account' ? '' : state.currentPassword,
        newPassword: action.section === 'account' ? '' : state.newPassword,
        confirmNewPassword: action.section === 'account' ? '' : state.confirmNewPassword,
        sections: {
          ...state.sections,
          [action.section]: {
            errorMessage: '',
            successMessage: action.message,
            isSubmitting: false,
          },
        },
      }
    case 'submit_failed':
      return {
        ...state,
        sections: {
          ...state.sections,
          [action.section]: {
            errorMessage: action.message,
            successMessage: '',
            isSubmitting: false,
          },
        },
      }
    case 'redirect_requested':
      return {
        ...state,
        navigationIntent: action.intent,
      }
  }
}
