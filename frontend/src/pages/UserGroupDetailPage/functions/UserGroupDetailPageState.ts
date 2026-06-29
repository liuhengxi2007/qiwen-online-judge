/**
 * 用户组详情页反馈消息状态，区分通用、保存和添加成员三个区域。
 */
export type UserGroupDetailPageMessageState = {
  generalErrorMessage: string
  generalSuccessMessage: string
  saveErrorMessage: string
  saveSuccessMessage: string
  addMemberErrorMessage: string
  addMemberSuccessMessage: string
}

/**
 * 用户组详情页反馈动作，覆盖保存、添加成员和通用操作的成功/失败/禁止状态。
 */
export type UserGroupDetailPageMessageAction =
  | { type: 'save_forbidden'; message: string }
  | { type: 'save_succeeded'; message: string }
  | { type: 'save_failed'; message: string }
  | { type: 'add_member_forbidden'; message: string }
  | { type: 'add_member_succeeded'; message: string }
  | { type: 'add_member_failed'; message: string }
  | { type: 'general_forbidden'; message: string }
  | { type: 'general_succeeded'; message: string }
  | { type: 'general_failed'; message: string }

/**
 * 用户组详情页反馈消息初始状态，所有区域默认无提示。
 */
export const initialUserGroupDetailPageMessageState: UserGroupDetailPageMessageState = {
  generalErrorMessage: '',
  generalSuccessMessage: '',
  saveErrorMessage: '',
  saveSuccessMessage: '',
  addMemberErrorMessage: '',
  addMemberSuccessMessage: '',
}

/**
 * 用户组详情页消息 reducer；按操作区域更新成功或错误文案，不影响业务数据。
 */
export function reduceUserGroupDetailPageMessageState(
  state: UserGroupDetailPageMessageState,
  action: UserGroupDetailPageMessageAction,
): UserGroupDetailPageMessageState {
  switch (action.type) {
    case 'save_forbidden':
      return {
        ...state,
        saveErrorMessage: action.message,
        saveSuccessMessage: '',
      }
    case 'save_succeeded':
      return {
        ...state,
        saveErrorMessage: '',
        saveSuccessMessage: action.message,
      }
    case 'save_failed':
      return {
        ...state,
        saveErrorMessage: action.message,
        saveSuccessMessage: '',
      }
    case 'add_member_forbidden':
      return {
        ...state,
        addMemberErrorMessage: action.message,
        addMemberSuccessMessage: '',
      }
    case 'add_member_succeeded':
      return {
        ...state,
        addMemberErrorMessage: '',
        addMemberSuccessMessage: action.message,
      }
    case 'add_member_failed':
      return {
        ...state,
        addMemberErrorMessage: action.message,
        addMemberSuccessMessage: '',
      }
    case 'general_forbidden':
      return {
        ...state,
        generalErrorMessage: action.message,
        generalSuccessMessage: '',
      }
    case 'general_succeeded':
      return {
        ...state,
        generalErrorMessage: '',
        generalSuccessMessage: action.message,
      }
    case 'general_failed':
      return {
        ...state,
        generalErrorMessage: action.message,
        generalSuccessMessage: '',
      }
  }
}
