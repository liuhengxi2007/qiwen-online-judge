/**
 * 题单详情页区块反馈状态，保存错误和成功消息。
 */
export type ProblemSetSectionMessageState = {
  errorMessage: string
  successMessage: string
}

/**
 * 题单详情页区块反馈动作，覆盖成功和失败消息。
 */
export type ProblemSetSectionMessageAction =
  | { type: 'clear' }
  | { type: 'set_error'; message: string }
  | { type: 'set_success'; message: string }

/**
 * 空题单区块反馈状态。
 */
export const emptyProblemSetSectionMessageState: ProblemSetSectionMessageState = {
  errorMessage: '',
  successMessage: '',
}

/**
 * 题单区块反馈 reducer；纯函数维护错误/成功文案。
 */
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
