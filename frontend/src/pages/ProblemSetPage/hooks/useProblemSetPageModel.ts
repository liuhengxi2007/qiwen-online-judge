import { useEffect, useReducer } from 'react'

import { ListProblemSets } from '@/apis/problemset/ListProblemSets'
import type { ProblemSetSummary } from '@/objects/problemset/response/ProblemSetSummary'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { sendAPI } from '@/system/api/api-message'

type ProblemSetPageState = {
  problemSets: ProblemSetSummary[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  errorMessage: string
}

type ProblemSetPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problemSets: ProblemSetSummary[]; page: number; pageSize: number; totalItems: number }
  | { type: 'load_failed'; message: string }

const initialState: ProblemSetPageState = {
  problemSets: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoading: true,
  errorMessage: '',
}

function reducer(state: ProblemSetPageState, action: ProblemSetPageAction): ProblemSetPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, problemSets: action.problemSets, page: action.page, pageSize: action.pageSize, totalItems: action.totalItems, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export function useProblemSetPageModel(pageRequest: PageRequest) {
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    const nextPageRequest = { page, pageSize }
    dispatch({ type: 'load_started' })
    void sendAPI(new ListProblemSets(nextPageRequest))
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problemSets: response.items, page: response.page, pageSize: response.pageSize, totalItems: response.totalItems })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load problem sets.' })
      })

    return () => {
      cancelled = true
    }
  }, [page, pageSize])

  return state
}
