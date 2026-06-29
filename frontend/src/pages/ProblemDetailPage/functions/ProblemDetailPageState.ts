/**
 * 题目详情页区块反馈状态，保存错误和成功消息。
 */
export type SectionMessageState = {
  errorMessage: string
  successMessage: string
}

/**
 * 题目详情页区块反馈动作，覆盖成功和失败消息。
 */
export type SectionMessageAction =
  | { type: 'clear' }
  | { type: 'set_error'; message: string }
  | { type: 'set_success'; message: string }

/**
 * 空的题目详情区块反馈状态。
 */
export const emptySectionMessageState: SectionMessageState = {
  errorMessage: '',
  successMessage: '',
}

/**
 * 题目详情区块反馈 reducer；纯函数维护错误/成功文案。
 */
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
