import { useCallback, useEffect, useReducer, useState } from 'react'

import { AddProblemToContest } from '@/apis/contest/AddProblemToContest'
import { GetContest } from '@/apis/contest/GetContest'
import { RegisterContest } from '@/apis/contest/RegisterContest'
import { UnregisterContest } from '@/apis/contest/UnregisterContest'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import { ListProblemSuggestions } from '@/apis/problem/ListProblemSuggestions'
import { parseProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type ContestDetailPageState = {
  contest: ContestDetail | null
  isLoading: boolean
  isRegistering: boolean
  isAttachingProblem: boolean
  loadErrorMessage: string
  registerErrorMessage: string
  registerSuccessMessage: string
  attachProblemErrorMessage: string
  attachProblemSuccessMessage: string
}

type ContestDetailPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; contest: ContestDetail }
  | { type: 'load_failed'; message: string }
  | { type: 'register_started' }
  | { type: 'register_succeeded'; contest: ContestDetail; message: string }
  | { type: 'register_failed'; message: string }
  | { type: 'attach_problem_started' }
  | { type: 'attach_problem_succeeded'; contest: ContestDetail; message: string }
  | { type: 'attach_problem_failed'; message: string }

const initialState: ContestDetailPageState = {
  contest: null,
  isLoading: true,
  isRegistering: false,
  isAttachingProblem: false,
  loadErrorMessage: '',
  registerErrorMessage: '',
  registerSuccessMessage: '',
  attachProblemErrorMessage: '',
  attachProblemSuccessMessage: '',
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
    case 'attach_problem_started':
      return { ...state, isAttachingProblem: true, attachProblemErrorMessage: '', attachProblemSuccessMessage: '' }
    case 'attach_problem_succeeded':
      return { ...state, isAttachingProblem: false, contest: action.contest, attachProblemErrorMessage: '', attachProblemSuccessMessage: action.message }
    case 'attach_problem_failed':
      return { ...state, isAttachingProblem: false, attachProblemErrorMessage: action.message, attachProblemSuccessMessage: '' }
  }
}

export function useContestDetailPageModel(contestSlug: ContestSlug) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, initialState)
  const [problemSearchInput, setProblemSearchInput] = useState('')
  const [isProblemSearchFocused, setIsProblemSearchFocused] = useState(false)
  const [isLoadingProblemSuggestions, setIsLoadingProblemSuggestions] = useState(false)
  const [problemSuggestions, setProblemSuggestions] = useState<ProblemSuggestion[]>([])

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

  useEffect(() => {
    if (!isProblemSearchFocused) {
      setProblemSuggestions([])
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      const parsedQuery = parseProblemSearchQuery(problemSearchInput)
      if (!parsedQuery.ok) {
        setProblemSuggestions([])
        return
      }

      setIsLoadingProblemSuggestions(true)
      void sendAPI(new ListProblemSuggestions(parsedQuery.value))
        .then((suggestions) => {
          if (!cancelled) {
            setProblemSuggestions(suggestions)
            setIsLoadingProblemSuggestions(false)
          }
        })
        .catch(() => {
          if (!cancelled) {
            setProblemSuggestions([])
            setIsLoadingProblemSuggestions(false)
          }
        })
    }, 150)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [isProblemSearchFocused, problemSearchInput])

  const attachProblem = useCallback(async () => {
    const parsedSlug = parseProblemSlug(problemSearchInput)
    if (!parsedSlug.ok) {
      dispatch({ type: 'attach_problem_failed', message: parsedSlug.error })
      return
    }

    dispatch({ type: 'attach_problem_started' })
    try {
      const contest = await sendAPI(new AddProblemToContest(contestSlug, { problemSlug: parsedSlug.value }))
      dispatch({ type: 'attach_problem_succeeded', contest, message: t('contest.detail.attachProblemSuccess') })
      setProblemSearchInput('')
      setProblemSuggestions([])
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.detail.attachProblemFailed')
      dispatch({ type: 'attach_problem_failed', message })
    }
  }, [contestSlug, problemSearchInput, t])

  const selectProblemSuggestion = useCallback((suggestion: ProblemSuggestion) => {
    setProblemSearchInput(problemSlugValue(suggestion.slug))
    setProblemSuggestions([])
    setIsProblemSearchFocused(false)
  }, [])

  return {
    ...state,
    problemSearchInput,
    isProblemSearchFocused,
    isLoadingProblemSuggestions,
    problemSuggestions: isProblemSearchFocused ? problemSuggestions : [],
    setProblemSearchInput,
    setIsProblemSearchFocused,
    selectProblemSuggestion,
    attachProblem,
    toggleRegistration,
  }
}
