import { useEffect, useReducer } from 'react'

import { GetContest } from '@/apis/contest/GetContest'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 比赛详情页状态，保存详情、加载状态和报名操作反馈。
 */
type ContestDetailPageState = {
  contest: ContestDetail | null
  isLoading: boolean
  loadErrorMessage: string
}

/**
 * 比赛详情 reducer 动作，覆盖详情加载和报名/取消报名流程。
 */
type ContestDetailPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; contest: ContestDetail }
  | { type: 'load_failed'; message: string }

const initialState: ContestDetailPageState = {
  contest: null,
  isLoading: true,
  loadErrorMessage: '',
}

/**
 * 比赛详情 reducer；纯函数维护详情和报名相关状态。
 */
function reducer(state: ContestDetailPageState, action: ContestDetailPageAction): ContestDetailPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, loadErrorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, contest: action.contest, loadErrorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, loadErrorMessage: action.message }
  }
}

/**
 * 比赛详情模型 hook；加载比赛详情并提供报名、取消报名动作。
 */
export function useContestDetailPageModel(contestSlug: ContestSlug) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void sendAPI(new GetContest(contestSlug))
      .then((contest) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', contest })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: t('contest.detail.loadFailed') })
      })

    return () => {
      cancelled = true
    }
  }, [contestSlug, t])

  return {
    ...state,
  }
}
