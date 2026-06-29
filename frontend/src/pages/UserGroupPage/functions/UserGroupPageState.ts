import type { UserGroupSummary } from '@/objects/usergroup/response/UserGroupSummary'

/**
 * 用户组列表页查询状态，保存分页响应、加载标记和错误消息。
 */
export type UserGroupPageState = {
  groups: UserGroupSummary[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  errorMessage: string
}

/**
 * 用户组列表 reducer 动作，覆盖加载开始、成功和失败。
 */
export type UserGroupPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; groups: UserGroupSummary[]; page: number; pageSize: number; totalItems: number }
  | { type: 'load_failed'; message: string }

/**
 * 用户组列表初始状态，默认第一页为空且处于加载中。
 */
export const initialUserGroupPageState: UserGroupPageState = {
  groups: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoading: true,
  errorMessage: '',
}

/**
 * 用户组列表 reducer；纯函数维护列表查询状态。
 */
export function reduceUserGroupPageState(
  state: UserGroupPageState,
  action: UserGroupPageAction,
): UserGroupPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return {
        groups: action.groups,
        page: action.page,
        pageSize: action.pageSize,
        totalItems: action.totalItems,
        isLoading: false,
        errorMessage: '',
      }
    case 'load_failed':
      return { ...state, groups: [], isLoading: false, errorMessage: action.message }
  }
}
