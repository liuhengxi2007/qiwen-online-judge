export type SectionMessageState = {
  errorMessage: string
  successMessage: string
}

export type SectionMessageAction =
  | { type: 'clear' }
  | { type: 'set_error'; message: string }
  | { type: 'set_success'; message: string }

export const emptySectionMessageState: SectionMessageState = {
  errorMessage: '',
  successMessage: '',
}

export function reduceSectionMessageState(
  state: SectionMessageState,
  action: SectionMessageAction,
): SectionMessageState {
  switch (action.type) {
    case 'clear':
      return emptySectionMessageState
    case 'set_error':
      return { ...state, errorMessage: action.message, successMessage: '' }
    case 'set_success':
      return { ...state, errorMessage: '', successMessage: action.message }
  }
}
