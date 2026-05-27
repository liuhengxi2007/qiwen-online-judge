export type CreateUserGroupPageState = {
  isSubmitting: boolean
  slug: string
  name: string
  description: string
  errorMessage: string
  successMessage: string
}

export type CreateUserGroupPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_name'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; message: string }
  | { type: 'submit_failed'; message: string }

export const initialCreateUserGroupPageState: CreateUserGroupPageState = {
  isSubmitting: false,
  slug: '',
  name: '',
  description: '',
  errorMessage: '',
  successMessage: '',
}

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
