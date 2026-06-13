import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { AppendRatingContestRequest } from '@/objects/rating/request/AppendRatingContestRequest'
import type { RatingManageState } from '@/objects/rating/response/RatingManageState'

/**
 * Rating 追加任务的表单草稿，保存比赛 slug 和 m 参数的原始输入。
 */
export type RatingManageDraft = {
  contestSlugInput: string
  mInput: string
}

/**
 * Rating 管理页状态，包含加载、追加、回退任务状态和页面反馈消息。
 */
export type RatingManagePageState = {
  draft: RatingManageDraft
  errorMessage: string
  isAppending: boolean
  isLoading: boolean
  isPopping: boolean
  manageState: RatingManageState | null
  noticeMessage: string
}

/**
 * Rating 管理页 reducer 动作，覆盖字段编辑、加载、追加和回退流程。
 */
export type RatingManagePageAction =
  | { type: 'set_contest_slug_input'; value: string }
  | { type: 'set_m_input'; value: string }
  | { type: 'load_started' }
  | { type: 'load_succeeded'; state: RatingManageState }
  | { type: 'load_failed'; message: string }
  | { type: 'append_started' }
  | { type: 'append_succeeded'; state: RatingManageState; message: string }
  | { type: 'append_failed'; message: string }
  | { type: 'pop_started' }
  | { type: 'pop_succeeded'; state: RatingManageState; message: string }
  | { type: 'pop_failed'; message: string }

/**
 * Rating 追加表单默认 m 值，和追加成功后重置值保持一致。
 */
export const defaultRatingMInput = '60'

/**
 * Rating 管理页初始状态，默认处于加载中并使用默认 m 输入。
 */
export const initialRatingManagePageState: RatingManagePageState = {
  draft: {
    contestSlugInput: '',
    mInput: defaultRatingMInput,
  },
  errorMessage: '',
  isAppending: false,
  isLoading: true,
  isPopping: false,
  manageState: null,
  noticeMessage: '',
}

/**
 * Rating 追加草稿校验结果，成功时携带后端请求体，失败时携带用户可见错误。
 */
type ValidateRatingAppendDraftResult =
  | { ok: true; request: AppendRatingContestRequest }
  | { ok: false; message: string }

/**
 * 校验 Rating 追加草稿；解析比赛 slug，并要求 m 是 2 到 100 的安全整数。
 */
export function validateRatingAppendDraft(draft: RatingManageDraft): ValidateRatingAppendDraftResult {
  const contestSlug = parseContestSlug(draft.contestSlugInput)
  if (!contestSlug.ok) {
    return { ok: false, message: contestSlug.error }
  }

  const m = Number.parseInt(draft.mInput, 10)
  if (!Number.isSafeInteger(m) || String(m) !== draft.mInput.trim()) {
    return { ok: false, message: 'Rating m must be an integer.' }
  }

  if (m < 2 || m > 100) {
    return { ok: false, message: 'Rating m must be between 2 and 100.' }
  }

  return {
    ok: true,
    request: {
      contestSlug: contestSlug.value,
      m,
    },
  }
}

/**
 * Rating 管理页 reducer；纯函数维护加载标记、草稿和成功/失败消息。
 */
export function ratingManagePageReducer(
  state: RatingManagePageState,
  action: RatingManagePageAction,
): RatingManagePageState {
  switch (action.type) {
    case 'set_contest_slug_input':
      return { ...state, draft: { ...state.draft, contestSlugInput: action.value } }
    case 'set_m_input':
      return { ...state, draft: { ...state.draft, mInput: action.value } }
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '', noticeMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, manageState: action.state, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
    case 'append_started':
      return { ...state, isAppending: true, errorMessage: '', noticeMessage: '' }
    case 'append_succeeded':
      return {
        ...state,
        draft: { contestSlugInput: '', mInput: defaultRatingMInput },
        isAppending: false,
        manageState: action.state,
        noticeMessage: action.message,
      }
    case 'append_failed':
      return { ...state, isAppending: false, errorMessage: action.message, noticeMessage: '' }
    case 'pop_started':
      return { ...state, isPopping: true, errorMessage: '', noticeMessage: '' }
    case 'pop_succeeded':
      return { ...state, isPopping: false, manageState: action.state, noticeMessage: action.message }
    case 'pop_failed':
      return { ...state, isPopping: false, errorMessage: action.message, noticeMessage: '' }
  }
}
