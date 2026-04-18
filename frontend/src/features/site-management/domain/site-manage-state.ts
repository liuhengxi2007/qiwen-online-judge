import { displayNameValue, type AuthUserListItem, type Username } from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'

export type SiteManageState = {
  statusMessage: string
  actionErrorMessage: string
  updatingUsername: Username | null
  navigationIntent: NavigationIntent | null
}

export type SiteManageAction =
  | { type: 'update_started'; username: Username }
  | { type: 'update_succeeded'; user: AuthUserListItem }
  | { type: 'delete_started'; username: Username }
  | { type: 'delete_succeeded'; message: string }
  | { type: 'update_failed'; message: string }
  | { type: 'redirect_requested'; intent: NavigationIntent }

export const initialSiteManageState: SiteManageState = {
  statusMessage: '',
  actionErrorMessage: '',
  updatingUsername: null,
  navigationIntent: null,
}

export function reduceSiteManageState(
  state: SiteManageState,
  action: SiteManageAction,
): SiteManageState {
  switch (action.type) {
    case 'update_started':
      return {
        ...state,
        updatingUsername: action.username,
        statusMessage: '',
        actionErrorMessage: '',
      }
    case 'update_succeeded':
      return {
        ...state,
        updatingUsername: null,
        statusMessage: `Permissions updated for ${displayNameValue(action.user.displayName)}.`,
        actionErrorMessage: '',
      }
    case 'delete_started':
      return {
        ...state,
        updatingUsername: action.username,
        statusMessage: '',
        actionErrorMessage: '',
      }
    case 'delete_succeeded':
      return {
        ...state,
        updatingUsername: null,
        statusMessage: action.message,
        actionErrorMessage: '',
      }
    case 'update_failed':
      return {
        ...state,
        updatingUsername: null,
        statusMessage: '',
        actionErrorMessage: action.message,
      }
    case 'redirect_requested':
      return {
        ...state,
        navigationIntent: action.intent,
      }
  }
}
