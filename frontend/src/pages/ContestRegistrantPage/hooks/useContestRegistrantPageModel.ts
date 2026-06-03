import { useEffect, useReducer } from 'react'

import { ListContestRegistrants } from '@/apis/contest/ListContestRegistrants'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestRegistrant } from '@/objects/contest/response/ContestRegistrant'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

type ContestRegistrantPageState = {
  registrants: ContestRegistrant[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  errorMessage: string
}

type ContestRegistrantPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; registrants: ContestRegistrant[]; page: number; pageSize: number; totalItems: number }
  | { type: 'load_failed'; message: string }

const initialState: ContestRegistrantPageState = {
  registrants: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoading: true,
  errorMessage: '',
}

function reducer(state: ContestRegistrantPageState, action: ContestRegistrantPageAction): ContestRegistrantPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, registrants: action.registrants, page: action.page, pageSize: action.pageSize, totalItems: action.totalItems, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export function useContestRegistrantPageModel(contestSlug: ContestSlug, pageRequest: PageRequest) {
  const { t } = useI18n()
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void sendAPI(new ListContestRegistrants(contestSlug, { page, pageSize }))
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', registrants: response.items, page: response.page, pageSize: response.pageSize, totalItems: response.totalItems })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: t('contest.registrants.loadFailed') })
      })

    return () => {
      cancelled = true
    }
  }, [contestSlug, page, pageSize, t])

  return state
}
