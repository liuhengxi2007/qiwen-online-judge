export type UserGroupDetailPageMessageState = {
  generalErrorMessage: string
  generalSuccessMessage: string
  saveErrorMessage: string
  saveSuccessMessage: string
  addMemberErrorMessage: string
  addMemberSuccessMessage: string
}

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

export const initialUserGroupDetailPageMessageState: UserGroupDetailPageMessageState = {
  generalErrorMessage: '',
  generalSuccessMessage: '',
  saveErrorMessage: '',
  saveSuccessMessage: '',
  addMemberErrorMessage: '',
  addMemberSuccessMessage: '',
}

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
