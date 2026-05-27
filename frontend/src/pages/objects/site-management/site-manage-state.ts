import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { AuthUserListItem } from '@/objects/user/response/AuthUserListItem'
import type { NavigationIntent } from '@/pages/objects/navigation-intent'

export type SiteManageNotice =
  | { kind: 'permissions_updated'; displayName: DisplayName }
  | { kind: 'text'; message: string }

export type SiteManageState = {
  notice: SiteManageNotice | null
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
  notice: null,
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
        notice: null,
        actionErrorMessage: '',
      }
    case 'update_succeeded':
      return {
        ...state,
        updatingUsername: null,
        notice: { kind: 'permissions_updated', displayName: action.user.displayName },
        actionErrorMessage: '',
      }
    case 'delete_started':
      return {
        ...state,
        updatingUsername: action.username,
        notice: null,
        actionErrorMessage: '',
      }
    case 'delete_succeeded':
      return {
        ...state,
        updatingUsername: null,
        notice: { kind: 'text', message: action.message },
        actionErrorMessage: '',
      }
    case 'update_failed':
      return {
        ...state,
        updatingUsername: null,
        notice: null,
        actionErrorMessage: action.message,
      }
    case 'redirect_requested':
      return {
        ...state,
        navigationIntent: action.intent,
      }
  }
}
