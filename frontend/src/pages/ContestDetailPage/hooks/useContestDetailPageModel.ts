import { useCallback, useEffect, useReducer } from 'react'

import { GetContest } from '@/apis/contest/GetContest'
import { RegisterContest } from '@/apis/contest/RegisterContest'
import { UnregisterContest } from '@/apis/contest/UnregisterContest'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type ContestDetailPageState = {
  contest: ContestDetail | null
  isLoading: boolean
  isRegistering: boolean
  loadErrorMessage: string
  registerErrorMessage: string
  registerSuccessMessage: string
}

type ContestDetailPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; contest: ContestDetail }
  | { type: 'load_failed'; message: string }
  | { type: 'register_started' }
  | { type: 'register_succeeded'; contest: ContestDetail; message: string }
  | { type: 'register_failed'; message: string }

const initialState: ContestDetailPageState = {
  contest: null,
  isLoading: true,
  isRegistering: false,
  loadErrorMessage: '',
  registerErrorMessage: '',
  registerSuccessMessage: '',
}

function reducer(state: ContestDetailPageState, action: ContestDetailPageAction): ContestDetailPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, loadErrorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, contest: action.contest, loadErrorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, loadErrorMessage: action.message }
    case 'register_started':
      return { ...state, isRegistering: true, registerErrorMessage: '', registerSuccessMessage: '' }
    case 'register_succeeded':
      return { ...state, isRegistering: false, contest: action.contest, registerErrorMessage: '', registerSuccessMessage: action.message }
    case 'register_failed':
      return { ...state, isRegistering: false, registerErrorMessage: action.message, registerSuccessMessage: '' }
  }
}

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

  const toggleRegistration = useCallback(async () => {
    const contest = state.contest
    if (!contest) {
      return
    }

    dispatch({ type: 'register_started' })
    try {
      const updatedContest = contest.registrationStatus.isRegistered
        ? await sendAPI(new UnregisterContest(contestSlug))
        : await sendAPI(new RegisterContest(contestSlug))
      dispatch({
        type: 'register_succeeded',
        contest: updatedContest,
        message: contest.registrationStatus.isRegistered ? t('contest.detail.unregisterSuccess') : t('contest.detail.registerSuccess'),
      })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.detail.registerFailed')
      dispatch({ type: 'register_failed', message })
    }
  }, [contestSlug, state.contest, t])

  return {
    ...state,
    toggleRegistration,
  }
}
