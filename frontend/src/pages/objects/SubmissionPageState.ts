/**
 * 提交列表用户名筛选草稿，区分输入框文本、已选用户名和焦点状态。
 */
export type UsernameFilterDraft = {
  query: string
  value: string
  selected: string | null
  focused: boolean
}

/**
 * 提交列表题目筛选草稿，记录当前输入文本和值以及建议面板焦点状态。
 */
export type ProblemFilterDraft = {
  query: string
  value: string
  focused: boolean
}

/**
 * 提交列表页面本地状态，集中保存两个筛选草稿和建议面板开关。
 */
export type SubmissionPageState = {
  usernameDraft: UsernameFilterDraft
  problemDraft: ProblemFilterDraft
  isUserSuggestionEnabled: boolean
  isProblemSuggestionEnabled: boolean
}

/**
 * 提交列表状态初始化参数，来自 URL 查询参数。
 */
export type SubmissionPageStateInit = {
  usernameQueryParam: string
  problemQueryParam: string
}

/**
 * 提交列表 reducer 动作集合，覆盖输入、焦点、建议选择、应用筛选和清空筛选。
 */
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

/**
 * 从 URL 查询参数创建提交列表初始状态；已有用户名筛选会同步为已选项。
 */
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

/**
 * 提交列表状态 reducer；纯函数更新筛选草稿和建议面板状态，不触发网络或路由副作用。
 */
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
