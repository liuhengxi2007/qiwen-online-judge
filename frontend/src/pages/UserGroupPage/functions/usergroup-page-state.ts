import type { UserGroupSummary } from '@/objects/usergroup/response/UserGroupSummary'

export type UserGroupPageState = {
  groups: UserGroupSummary[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  errorMessage: string
}

export type UserGroupPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; groups: UserGroupSummary[]; page: number; pageSize: number; totalItems: number }
  | { type: 'load_failed'; message: string }

export const initialUserGroupPageState: UserGroupPageState = {
  groups: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoading: true,
  errorMessage: '',
}

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
