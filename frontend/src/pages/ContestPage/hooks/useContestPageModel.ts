import { useEffect, useReducer } from 'react'

import { ListContests } from '@/apis/contest/ListContests'
import { RegisterContest } from '@/apis/contest/RegisterContest'
import { UnregisterContest } from '@/apis/contest/UnregisterContest'
import type { ContestSummary } from '@/objects/contest/response/ContestSummary'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type ContestPageState = {
  contests: ContestSummary[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  activeRegistrationSlug: string | null
  errorMessage: string
  registrationMessage: string
}

type ContestPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; contests: ContestSummary[]; page: number; pageSize: number; totalItems: number }
  | { type: 'load_failed'; message: string }
  | { type: 'registration_started'; slug: string }
  | { type: 'registration_succeeded'; contest: ContestSummary; message: string }
  | { type: 'registration_failed'; message: string }

const initialState: ContestPageState = {
  contests: [],
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoading: true,
  activeRegistrationSlug: null,
  errorMessage: '',
  registrationMessage: '',
}

function reducer(state: ContestPageState, action: ContestPageAction): ContestPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, contests: action.contests, page: action.page, pageSize: action.pageSize, totalItems: action.totalItems, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
    case 'registration_started':
      return { ...state, activeRegistrationSlug: action.slug, errorMessage: '', registrationMessage: '' }
    case 'registration_succeeded':
      return {
        ...state,
        activeRegistrationSlug: null,
        registrationMessage: action.message,
        contests: state.contests.map((contest) => contest.id === action.contest.id ? action.contest : contest),
      }
    case 'registration_failed':
      return { ...state, activeRegistrationSlug: null, errorMessage: action.message, registrationMessage: '' }
  }
}

export function useContestPageModel(pageRequest: PageRequest) {
  const { t } = useI18n()
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    const nextPageRequest = { page, pageSize }
    dispatch({ type: 'load_started' })
    void sendAPI(new ListContests(nextPageRequest))
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', contests: response.items, page: response.page, pageSize: response.pageSize, totalItems: response.totalItems })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: t('contest.list.loadFailed') })
      })

    return () => {
      cancelled = true
    }
  }, [page, pageSize, t])

  const toggleRegistration = async (contest: ContestSummary) => {
    dispatch({ type: 'registration_started', slug: contest.slug })
    try {
      const registrationStatus = contest.registrationStatus.isRegistered
        ? await sendAPI(new UnregisterContest(contest.slug))
        : await sendAPI(new RegisterContest(contest.slug))
      dispatch({
        type: 'registration_succeeded',
        contest: { ...contest, registrationStatus },
        message: contest.registrationStatus.isRegistered ? t('contest.list.unregisterSuccess') : t('contest.list.registerSuccess'),
      })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.list.registrationFailed')
      dispatch({ type: 'registration_failed', message })
    }
  }

  return {
    ...state,
    toggleRegistration,
  }
}
