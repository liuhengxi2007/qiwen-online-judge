import { useCallback, useEffect, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { addProblemToProblemSet, getProblemSet } from '@/features/problemset/api/problemset-client'
import { validateProblemSetLinkDraft } from '@/features/problemset/domain/problemset-link-form'
import type { ProblemSetDetail, ProblemSetSlug } from '@/features/problemset/domain/problemset'

type ProblemSetDetailPageState = {
  problemSet: ProblemSetDetail | null
  isLoading: boolean
  activeLink: boolean
  linkProblemSlug: string
  errorMessage: string
  successMessage: string
}

type ProblemSetDetailPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problemSet: ProblemSetDetail }
  | { type: 'load_failed'; message: string }
  | { type: 'set_link_problem_slug'; value: string }
  | { type: 'link_started' }
  | { type: 'link_succeeded'; problemSet: ProblemSetDetail }
  | { type: 'link_failed'; message: string }

const initialState: ProblemSetDetailPageState = {
  problemSet: null,
  isLoading: true,
  activeLink: false,
  linkProblemSlug: '',
  errorMessage: '',
  successMessage: '',
}

function reducer(state: ProblemSetDetailPageState, action: ProblemSetDetailPageAction): ProblemSetDetailPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '', successMessage: '' }
    case 'load_succeeded':
      return { ...state, problemSet: action.problemSet, isLoading: false, errorMessage: '', successMessage: '' }
    case 'load_failed':
      return { ...state, problemSet: null, isLoading: false, errorMessage: action.message, successMessage: '' }
    case 'set_link_problem_slug':
      return { ...state, linkProblemSlug: action.value }
    case 'link_started':
      return { ...state, activeLink: true, errorMessage: '', successMessage: '' }
    case 'link_succeeded':
      return {
        ...state,
        activeLink: false,
        problemSet: action.problemSet,
        linkProblemSlug: '',
        errorMessage: '',
        successMessage: 'Problem linked to problem set.',
      }
    case 'link_failed':
      return { ...state, activeLink: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useProblemSetDetailPageModel(problemSetSlug: ProblemSetSlug, canManageProblems: boolean) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void getProblemSet(problemSetSlug)
      .then((problemSet) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problemSet })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load problem set details.' })
      })

    return () => {
      cancelled = true
    }
  }, [problemSetSlug])

  const attachProblem = useCallback(async () => {
    if (!canManageProblems) {
      dispatch({ type: 'link_failed', message: 'Problem manager permission required.' })
      return
    }

    const validation = validateProblemSetLinkDraft({
      problemSlug: state.linkProblemSlug,
    })
    if (!validation.ok) {
      dispatch({ type: 'link_failed', message: validation.message })
      return
    }

    dispatch({ type: 'link_started' })

    try {
      const updatedProblemSet = await addProblemToProblemSet(problemSetSlug, validation.request)
      dispatch({ type: 'link_succeeded', problemSet: updatedProblemSet })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to link problem to problem set.'
      dispatch({ type: 'link_failed', message })
    }
  }, [canManageProblems, problemSetSlug, state.linkProblemSlug])

  return {
    ...state,
    setLinkProblemSlug: (value: string) => dispatch({ type: 'set_link_problem_slug', value }),
    attachProblem,
  }
}
