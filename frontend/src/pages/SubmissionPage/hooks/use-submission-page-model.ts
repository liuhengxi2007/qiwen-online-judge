import { useReducer, type KeyboardEvent } from 'react'
import { useSearchParams } from 'react-router-dom'

import { problemSlugValue } from '@/objects/problem/problem-parsers'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useSubmissionListQuery } from './use-submission-list-query'
import { useSubmissionSuggestions } from './use-submission-suggestions'
import {
  buildSubmissionListRequest,
  defaultSortDirection,
  readSubmissionSort,
  readSubmissionSortDirection,
  readSubmissionVerdictFilter,
  shouldShowTypingSuggestions,
  submissionSortValues,
  verdictFilterLabel,
  verdictFilterValues,
  type SubmissionSort,
  type SubmissionSortDirection,
  type SubmissionVerdictFilter,
} from '../functions/submission-page-support'
import {
  createSubmissionPageState,
  submissionPageReducer,
} from '../functions/submission-page-state'
import {
  buildPageNumbers,
  calculateTotalPages,
  parsePositivePage,
} from '@/objects/shared/pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/use-page-search-param-correction'

export function useSubmissionPageModel(fixedProblemSlugFilter?: ProblemSlug) {
  const [searchParams, setSearchParams] = useSearchParams()
  const usernameQueryParam = searchParams.get('username')?.trim() ?? ''
  const problemQueryParam = searchParams.get('problem')?.trim() ?? ''
  const [pageState, dispatchPageState] = useReducer(
    submissionPageReducer,
    { usernameQueryParam, problemQueryParam },
    createSubmissionPageState,
  )
  const {
    usernameDraft,
    problemDraft,
    isUserSuggestionEnabled,
    isProblemSuggestionEnabled,
  } = pageState
  const hasFixedProblemFilter = fixedProblemSlugFilter !== undefined
  const activeProblemQuery = hasFixedProblemFilter ? problemSlugValue(fixedProblemSlugFilter) : problemQueryParam
  const usernameFilterInput = usernameDraft.query === usernameQueryParam ? usernameDraft.value : usernameQueryParam
  const selectedUsernameSuggestion =
    usernameDraft.query === usernameQueryParam ? usernameDraft.selected : usernameQueryParam || null
  const isUsernameFilterFocused = usernameDraft.query === usernameQueryParam && usernameDraft.focused
  const problemFilterInput =
    hasFixedProblemFilter
      ? activeProblemQuery
      : problemDraft.query === problemQueryParam
        ? problemDraft.value
        : problemQueryParam
  const isProblemFilterFocused = !hasFixedProblemFilter && problemDraft.query === problemQueryParam && problemDraft.focused
  const activeSort = readSubmissionSort(searchParams)
  const activeDirection = readSubmissionSortDirection(searchParams, activeSort)
  const activeVerdictFilter = readSubmissionVerdictFilter(searchParams)
  const currentPage = parsePositivePage(searchParams.get('page'))
  const request = buildSubmissionListRequest({
    usernameQueryParam,
    activeProblemQuery,
    activeVerdictFilter,
    activeSort,
    activeDirection,
    currentPage,
  })
  const submissionQuery = useSubmissionListQuery(request)
  const currentPageSubmissions = submissionQuery.response.items
  const totalPages = calculateTotalPages(submissionQuery.response.totalItems, submissionQuery.response.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)
  const showUserSuggestionPanel =
    isUserSuggestionEnabled && isUsernameFilterFocused && shouldShowTypingSuggestions(usernameFilterInput)
  const showProblemSuggestionPanel =
    isProblemSuggestionEnabled && isProblemFilterFocused && shouldShowTypingSuggestions(problemFilterInput)
  const {
    isLoadingUserSuggestions,
    isLoadingProblemSuggestions,
    userSuggestions,
    problemSuggestions,
  } = useSubmissionSuggestions({
    usernameFilterInput,
    problemFilterInput,
    showUserSuggestionPanel,
    showProblemSuggestionPanel,
  })

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: submissionQuery.isLoading,
    setSearchParams,
  })

  function updateSearchFilter(name: string, value: string | null) {
    const nextSearchParams = new URLSearchParams(searchParams)
    if (value === null || !value.trim()) {
      nextSearchParams.delete(name)
    } else {
      nextSearchParams.set(name, value)
    }
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  function updateUsernameFilterInput(value: string) {
    dispatchPageState({ type: 'usernameInputChanged', query: usernameQueryParam, value })
  }

  function updateProblemFilterInput(value: string) {
    dispatchPageState({ type: 'problemInputChanged', query: problemQueryParam, value })
  }

  function applyFilters() {
    const trimmedUsernameInput = usernameFilterInput.trim()
    const nextSearchParams = new URLSearchParams(searchParams)

    if (!trimmedUsernameInput) {
      nextSearchParams.delete('username')
    } else {
      nextSearchParams.set('username', selectedUsernameSuggestion ?? trimmedUsernameInput)
    }

    if (!hasFixedProblemFilter) {
      const trimmedProblemInput = problemFilterInput.trim()
      if (!trimmedProblemInput) {
        nextSearchParams.delete('problem')
      } else {
        nextSearchParams.set('problem', trimmedProblemInput)
      }
    }

    nextSearchParams.delete('page')
    dispatchPageState({
      type: 'filtersApplied',
      usernameQuery: selectedUsernameSuggestion ?? trimmedUsernameInput,
      problemQuery: hasFixedProblemFilter ? problemQueryParam : problemFilterInput.trim(),
    })
    setSearchParams(nextSearchParams)
  }

  function clearFilters() {
    dispatchPageState({ type: 'filtersCleared' })
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.delete('username')
    nextSearchParams.delete('problem')
    nextSearchParams.delete('verdict')
    nextSearchParams.delete('sort')
    nextSearchParams.delete('direction')
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  function applyFiltersOnEnter(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') {
      return
    }

    event.preventDefault()
    applyFilters()
  }

  function goToPage(page: number) {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  function changeSort(value: SubmissionSort) {
    const nextSearchParams = new URLSearchParams(searchParams)
    if (value === 'submitted') {
      nextSearchParams.delete('sort')
    } else {
      nextSearchParams.set('sort', value)
    }
    nextSearchParams.delete('direction')
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  function toggleDirection() {
    const nextDirection: SubmissionSortDirection = activeDirection === 'asc' ? 'desc' : 'asc'
    updateSearchFilter('direction', nextDirection === defaultSortDirection(activeSort) ? null : nextDirection)
  }

  return {
    usernameQueryParam,
    usernameFilterInput,
    problemFilterInput,
    isUsernameFilterFocused,
    isProblemFilterFocused,
    isUserSuggestionEnabled,
    isProblemSuggestionEnabled,
    isLoadingUserSuggestions,
    isLoadingProblemSuggestions,
    userSuggestions,
    problemSuggestions,
    hasFixedProblemFilter,
    activeProblemQuery,
    activeSort,
    activeDirection,
    activeVerdictFilter,
    currentPage,
    currentPageSubmissions,
    totalPages,
    pageNumbers,
    showUserSuggestionPanel,
    showProblemSuggestionPanel,
    submissionQuery,
    verdictFilterValues,
    submissionSortValues,
    verdictFilterLabel,
    updateUsernameFilterInput,
    updateProblemFilterInput,
    setIsUsernameFilterFocused: (focused: boolean) => {
      dispatchPageState({ type: 'usernameFocusChanged', query: usernameQueryParam, focused })
    },
    setIsProblemFilterFocused: (focused: boolean) => {
      dispatchPageState({ type: 'problemFocusChanged', query: problemQueryParam, focused })
    },
    setIsUserSuggestionEnabled: (enabled: boolean) => {
      dispatchPageState({ type: 'userSuggestionEnabledChanged', query: usernameQueryParam, enabled })
    },
    setIsProblemSuggestionEnabled: (enabled: boolean) => {
      dispatchPageState({ type: 'problemSuggestionEnabledChanged', query: problemQueryParam, enabled })
    },
    selectUsernameSuggestion: (username: string) => {
      dispatchPageState({ type: 'usernameSuggestionSelected', query: usernameQueryParam, username })
    },
    selectProblemSuggestion: (slug: string) => {
      dispatchPageState({ type: 'problemSuggestionSelected', query: problemQueryParam, slug })
    },
    applyFilters,
    clearFilters,
    applyFiltersOnEnter,
    goToPage,
    changeSort,
    toggleDirection,
    updateVerdictFilter: (value: SubmissionVerdictFilter) =>
      updateSearchFilter('verdict', value === 'all' ? null : value),
  }
}
