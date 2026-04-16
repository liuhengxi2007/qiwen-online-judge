export type ProblemSetSectionMessageState = {
  errorMessage: string
  successMessage: string
}

export type ProblemSetSectionMessageAction =
  | { type: 'clear' }
  | { type: 'set_error'; message: string }
  | { type: 'set_success'; message: string }

export const emptyProblemSetSectionMessageState: ProblemSetSectionMessageState = {
  errorMessage: '',
  successMessage: '',
}

export function reduceProblemSetSectionMessageState(
  state: ProblemSetSectionMessageState,
  action: ProblemSetSectionMessageAction,
): ProblemSetSectionMessageState {
  switch (action.type) {
    case 'clear':
      return emptyProblemSetSectionMessageState
    case 'set_error':
      return { ...state, errorMessage: action.message, successMessage: '' }
    case 'set_success':
      return { ...state, errorMessage: '', successMessage: action.message }
  }
}
