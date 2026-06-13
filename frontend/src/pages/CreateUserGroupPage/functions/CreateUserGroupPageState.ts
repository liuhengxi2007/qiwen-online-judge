/**
 * 创建用户组页状态，保存草稿、提交状态和反馈消息。
 */
export type CreateUserGroupPageState = {
  isSubmitting: boolean
  slug: string
  name: string
  description: string
  errorMessage: string
  successMessage: string
}

/**
 * 创建用户组页 reducer 动作，覆盖字段编辑和提交成功/失败。
 */
export type CreateUserGroupPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_name'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; message: string }
  | { type: 'submit_failed'; message: string }

/**
 * 创建用户组页初始状态，默认表单为空。
 */
export const initialCreateUserGroupPageState: CreateUserGroupPageState = {
  isSubmitting: false,
  slug: '',
  name: '',
  description: '',
  errorMessage: '',
  successMessage: '',
}

/**
 * 根据操作结果重置创建用户组页状态，成功时清空草稿并写入成功消息。
 */
export function resetCreateUserGroupPageState(
  state: CreateUserGroupPageState,
  successMessage: string,
): CreateUserGroupPageState {
  return {
    ...state,
    isSubmitting: false,
    slug: '',
    name: '',
    description: '',
    errorMessage: '',
    successMessage,
  }
}

/**
 * 创建用户组页 reducer；纯函数维护草稿、提交状态和反馈。
 */
export function reduceCreateUserGroupPageState(
  state: CreateUserGroupPageState,
  action: CreateUserGroupPageAction,
): CreateUserGroupPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, slug: action.value }
    case 'set_name':
      return { ...state, name: action.value }
    case 'set_description':
      return { ...state, description: action.value }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return resetCreateUserGroupPageState(state, action.message)
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}
