import { useEffect, useReducer } from 'react'

import { ListContestRanklist } from '@/apis/contest/ListContestRanklist'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestRanklistItem } from '@/objects/contest/response/ContestRanklistItem'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

type ContestRanklistPageState = {
  items: ContestRanklistItem[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  errorMessage: string
}

type ContestRanklistPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; items: ContestRanklistItem[]; page: number; pageSize: number; totalItems: number }
  | { type: 'load_failed'; message: string }

const initialState: ContestRanklistPageState = {
  items: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoading: true,
  errorMessage: '',
}

function reducer(state: ContestRanklistPageState, action: ContestRanklistPageAction): ContestRanklistPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, items: action.items, page: action.page, pageSize: action.pageSize, totalItems: action.totalItems, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export function useContestRanklistPageModel(contestSlug: ContestSlug, pageRequest: PageRequest) {
  const { t } = useI18n()
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void sendAPI(new ListContestRanklist(contestSlug, { page, pageSize }))
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', items: response.items, page: response.page, pageSize: response.pageSize, totalItems: response.totalItems })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: t('contest.ranklist.loadFailed') })
      })

    return () => {
      cancelled = true
    }
  }, [contestSlug, page, pageSize, t])

  return state
}
