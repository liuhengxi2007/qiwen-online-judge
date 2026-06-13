import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'

/**
 * 站点管理页通知类型，区分权限保存、删除用户和错误反馈。
 */
export type SiteManageNotice =
  | { kind: 'permissions_updated'; displayName: DisplayName }
  | { kind: 'text'; message: string }

/**
 * 站点管理页本地状态，保存正在操作的用户和最近通知。
 */
export type SiteManageState = {
  notice: SiteManageNotice | null
  actionErrorMessage: string
  updatingUsername: Username | null
  navigationIntent: NavigationIntent | null
}

/**
 * 站点管理页 reducer 动作，覆盖用户操作开始、成功、失败和通知清理。
 */
export type SiteManageAction =
  | { type: 'update_started'; username: Username }
  | { type: 'update_succeeded'; user: ManagedUserListItem }
  | { type: 'delete_started'; username: Username }
  | { type: 'delete_succeeded'; message: string }
  | { type: 'update_failed'; message: string }
  | { type: 'redirect_requested'; intent: NavigationIntent }

/**
 * 站点管理页初始状态，默认没有进行中的用户操作。
 */
export const initialSiteManageState: SiteManageState = {
  notice: null,
  actionErrorMessage: '',
  updatingUsername: null,
  navigationIntent: null,
}

/**
 * 站点管理页 reducer；纯函数维护操作中用户和通知消息。
 */
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
