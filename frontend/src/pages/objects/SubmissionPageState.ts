export type UsernameFilterDraft = {
  query: string
  value: string
  selected: string | null
  focused: boolean
}

export type ProblemFilterDraft = {
  query: string
  value: string
  focused: boolean
}

export type SubmissionPageState = {
  usernameDraft: UsernameFilterDraft
  problemDraft: ProblemFilterDraft
  isUserSuggestionEnabled: boolean
  isProblemSuggestionEnabled: boolean
}

export type SubmissionPageStateInit = {
  usernameQueryParam: string
  problemQueryParam: string
}

export type SubmissionPageAction =
  | { type: 'usernameInputChanged'; query: string; value: string }
  | { type: 'problemInputChanged'; query: string; value: string }
  | { type: 'usernameFocusChanged'; query: string; focused: boolean }
  | { type: 'problemFocusChanged'; query: string; focused: boolean }
  | { type: 'userSuggestionEnabledChanged'; query: string; enabled: boolean }
  | { type: 'problemSuggestionEnabledChanged'; query: string; enabled: boolean }
  | { type: 'usernameSuggestionSelected'; query: string; username: string }
  | { type: 'problemSuggestionSelected'; query: string; slug: string }
  | { type: 'filtersApplied'; usernameQuery: string; problemQuery: string }
  | { type: 'filtersCleared' }

export function createSubmissionPageState({
  usernameQueryParam,
  problemQueryParam,
}: SubmissionPageStateInit): SubmissionPageState {
  return {
    usernameDraft: {
      query: usernameQueryParam,
      value: usernameQueryParam,
      selected: usernameQueryParam || null,
      focused: false,
    },
    problemDraft: {
      query: problemQueryParam,
      value: problemQueryParam,
      focused: false,
    },
    isUserSuggestionEnabled: false,
    isProblemSuggestionEnabled: false,
  }
}

export function submissionPageReducer(state: SubmissionPageState, action: SubmissionPageAction): SubmissionPageState {
  switch (action.type) {
    case 'usernameInputChanged':
      return {
        ...state,
        usernameDraft: { query: action.query, value: action.value, selected: null, focused: true },
      }
    case 'problemInputChanged':
      return {
        ...state,
        problemDraft: { query: action.query, value: action.value, focused: true },
      }
    case 'usernameFocusChanged':
      return {
        ...state,
        usernameDraft: { ...state.usernameDraft, query: action.query, focused: action.focused },
      }
    case 'problemFocusChanged':
      return {
        ...state,
        problemDraft: { ...state.problemDraft, query: action.query, focused: action.focused },
      }
    case 'userSuggestionEnabledChanged':
      return {
        ...state,
        isUserSuggestionEnabled: action.enabled,
        usernameDraft: { ...state.usernameDraft, query: action.query, focused: action.enabled },
      }
    case 'problemSuggestionEnabledChanged':
      return {
        ...state,
        isProblemSuggestionEnabled: action.enabled,
        problemDraft: { ...state.problemDraft, query: action.query, focused: action.enabled },
      }
    case 'usernameSuggestionSelected':
      return {
        ...state,
        usernameDraft: {
          query: action.query,
          value: action.username,
          selected: action.username,
          focused: false,
        },
      }
    case 'problemSuggestionSelected':
      return {
        ...state,
        problemDraft: {
          query: action.query,
          value: action.slug,
          focused: false,
        },
      }
    case 'filtersApplied':
      return {
        ...state,
        usernameDraft: {
          query: action.usernameQuery,
          value: action.usernameQuery,
          selected: action.usernameQuery || null,
          focused: false,
        },
        problemDraft: {
          query: action.problemQuery,
          value: action.problemQuery,
          focused: false,
        },
      }
    case 'filtersCleared':
      return {
        ...state,
        usernameDraft: { query: '', value: '', selected: null, focused: false },
        problemDraft: { query: '', value: '', focused: false },
      }
  }
}
