import { useEffect, useState, type KeyboardEvent } from 'react'
import { useSearchParams } from 'react-router-dom'

import { listProblemSuggestions } from '@/features/problem/api/problem-client'
import type { ProblemSuggestion } from '@/features/problem/domain/problem'
import {
  formatProblemTitleDisplay,
  problemSlugValue,
  problemTitleValue,
  type ProblemSlug,
  useProblemTitleDisplayMode,
} from '@/features/problem/domain/problem'
import { listUserSuggestions } from '@/features/user/api/user-client'
import type { UserIdentity } from '@/features/user/domain/user'
import {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
} from '@/features/submission/components/submission-support'
import { useSubmissionListQuery } from '@/features/submission/hooks/use-submission-list-query'
import {
  isSubmissionSort,
  isSubmissionSortDirection,
  isSubmissionVerdictFilter,
  parseSubmissionProblemQuery,
  parseSubmissionUserQuery,
  submissionIdValue,
  submissionLanguageLabel,
  submissionStatusLabel,
  submissionVerdictLabel,
  type SubmissionListRequest,
  type SubmissionSort,
  type SubmissionSortDirection,
  type SubmissionSummary,
  type SubmissionVerdictFilter,
} from '@/features/submission/domain/submission'
import {
  buildPageNumbers,
  calculateTotalPages,
  getPageCorrection,
  parsePositivePage,
} from '@/shared/domain/pagination'

function defaultSortDirection(sort: SubmissionSort): SubmissionSortDirection {
  switch (sort) {
    case 'submitted':
      return 'desc'
    case 'time':
    case 'memory':
    case 'code_length':
      return 'asc'
  }
}

function shouldShowTypingSuggestions(value: string): boolean {
  return value.trim().length > 0
}

const submissionsPerPage = 10

export const verdictFilterValues = [
  'all',
  'pending',
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]

export const submissionSortValues = [
  'submitted',
  'time',
  'memory',
  'code_length',
] as const satisfies readonly SubmissionSort[]

export type { ProblemSuggestion, SubmissionSort, SubmissionSortDirection, SubmissionSummary, SubmissionVerdictFilter }
export {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatProblemTitleDisplay,
  problemSlugValue,
  problemTitleValue,
  submissionIdValue,
  submissionLanguageLabel,
  submissionStatusLabel,
  submissionVerdictLabel,
  useProblemTitleDisplayMode,
}

export function useSubmissionPageModel(fixedProblemSlugFilter?: ProblemSlug) {
  const [searchParams, setSearchParams] = useSearchParams()
  const usernameQueryParam = searchParams.get('username')?.trim() ?? ''
  const problemQueryParam = searchParams.get('problem')?.trim() ?? ''
  const [usernameDraft, setUsernameDraft] = useState({
    query: usernameQueryParam,
    value: usernameQueryParam,
    selected: usernameQueryParam || null,
    focused: false,
  })
  const [problemDraft, setProblemDraft] = useState({
    query: problemQueryParam,
    value: problemQueryParam,
    focused: false,
  })
  const [isUserSuggestionEnabled, setIsUserSuggestionEnabled] = useState(false)
  const [isProblemSuggestionEnabled, setIsProblemSuggestionEnabled] = useState(false)
  const [isLoadingUserSuggestions, setIsLoadingUserSuggestions] = useState(false)
  const [isLoadingProblemSuggestions, setIsLoadingProblemSuggestions] = useState(false)
  const [userSuggestions, setUserSuggestions] = useState<UserIdentity[]>([])
  const [problemSuggestions, setProblemSuggestions] = useState<ProblemSuggestion[]>([])
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
  const activeSort = (() => {
    const rawSort = searchParams.get('sort')
    return rawSort && isSubmissionSort(rawSort) ? rawSort : 'submitted'
  })()
  const activeDirection = (() => {
    const rawDirection = searchParams.get('direction')
    return rawDirection && isSubmissionSortDirection(rawDirection) ? rawDirection : defaultSortDirection(activeSort)
  })()
  const activeVerdictFilter = (() => {
    const rawVerdict = searchParams.get('verdict')
    return rawVerdict && isSubmissionVerdictFilter(rawVerdict) ? rawVerdict : 'all'
  })()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const request: SubmissionListRequest = {
    userQuery: (() => {
      if (!usernameQueryParam) {
        return null
      }
      const parsedQuery = parseSubmissionUserQuery(usernameQueryParam)
      return parsedQuery.ok ? parsedQuery.value : null
    })(),
    problemQuery: (() => {
      if (!activeProblemQuery) {
        return null
      }
      const parsedQuery = parseSubmissionProblemQuery(activeProblemQuery)
      return parsedQuery.ok ? parsedQuery.value : null
    })(),
    verdict: activeVerdictFilter,
    sort: activeSort,
    direction: activeDirection,
    pageRequest: {
      page: currentPage,
      pageSize: submissionsPerPage,
    },
  }
  const submissionQuery = useSubmissionListQuery(request)
  const currentPageSubmissions = submissionQuery.response.items
  const totalPages = calculateTotalPages(submissionQuery.response.totalItems, submissionQuery.response.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)
  const showUserSuggestionPanel =
    isUserSuggestionEnabled && isUsernameFilterFocused && shouldShowTypingSuggestions(usernameFilterInput)
  const showProblemSuggestionPanel =
    isProblemSuggestionEnabled && isProblemFilterFocused && shouldShowTypingSuggestions(problemFilterInput)
  const visibleUserSuggestions = showUserSuggestionPanel ? userSuggestions : []
  const visibleProblemSuggestions = showProblemSuggestionPanel ? problemSuggestions : []

  useEffect(() => {
    if (submissionQuery.isLoading) {
      return
    }

    const correction = getPageCorrection(currentPage, totalPages)
    if (correction.kind === 'none') {
      return
    }

    const nextSearchParams = new URLSearchParams(searchParams)
    if (correction.kind === 'delete') {
      nextSearchParams.delete('page')
    } else {
      nextSearchParams.set('page', String(correction.page))
    }
    setSearchParams(nextSearchParams)
  }, [currentPage, searchParams, setSearchParams, submissionQuery.isLoading, totalPages])

  useEffect(() => {
    if (!isUserSuggestionEnabled || !isUsernameFilterFocused || !shouldShowTypingSuggestions(usernameFilterInput)) {
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      setIsLoadingUserSuggestions(true)
      void listUserSuggestions(usernameFilterInput.trim())
        .then((suggestions) => {
          if (!cancelled) {
            setUserSuggestions(suggestions)
            setIsLoadingUserSuggestions(false)
          }
        })
        .catch(() => {
          if (!cancelled) {
            setUserSuggestions([])
            setIsLoadingUserSuggestions(false)
          }
        })
    }, 150)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [isUserSuggestionEnabled, isUsernameFilterFocused, usernameFilterInput])

  useEffect(() => {
    if (!isProblemSuggestionEnabled || !isProblemFilterFocused || !shouldShowTypingSuggestions(problemFilterInput) || hasFixedProblemFilter) {
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      setIsLoadingProblemSuggestions(true)
      void listProblemSuggestions(problemFilterInput.trim())
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
  }, [hasFixedProblemFilter, isProblemSuggestionEnabled, isProblemFilterFocused, problemFilterInput])

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
    setUsernameDraft({ query: usernameQueryParam, value, selected: null, focused: true })
  }

  function updateProblemFilterInput(value: string) {
    setProblemDraft({ query: problemQueryParam, value, focused: true })
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
    const nextUsernameQuery = selectedUsernameSuggestion ?? trimmedUsernameInput
    setUsernameDraft({
      query: nextUsernameQuery,
      value: nextUsernameQuery,
      selected: nextUsernameQuery || null,
      focused: false,
    })
    setProblemDraft({
      query: hasFixedProblemFilter ? problemQueryParam : problemFilterInput.trim(),
      value: hasFixedProblemFilter ? problemQueryParam : problemFilterInput.trim(),
      focused: false,
    })
    setSearchParams(nextSearchParams)
  }

  function clearFilters() {
    setUsernameDraft({ query: '', value: '', selected: null, focused: false })
    setProblemDraft({ query: '', value: '', focused: false })
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
    const nextDirection = defaultSortDirection(value)
    nextSearchParams.delete('direction')
    if (nextDirection !== defaultSortDirection(value)) {
      nextSearchParams.set('direction', nextDirection)
    }
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  function toggleDirection() {
    const nextDirection: SubmissionSortDirection = activeDirection === 'asc' ? 'desc' : 'asc'
    updateSearchFilter('direction', nextDirection === defaultSortDirection(activeSort) ? null : nextDirection)
  }

  function verdictFilterLabel(verdict: SubmissionVerdictFilter, allVerdictsLabel: string): string {
    if (verdict === 'all') {
      return allVerdictsLabel
    }
    if (verdict === 'pending') {
      return submissionVerdictLabel(null)
    }
    return submissionVerdictLabel(verdict)
  }

  return {
    usernameQueryParam,
    usernameFilterInput,
    problemFilterInput,
    isUsernameFilterFocused,
    isProblemFilterFocused,
    isUserSuggestionEnabled,
    isProblemSuggestionEnabled,
    isLoadingUserSuggestions: showUserSuggestionPanel && isLoadingUserSuggestions,
    isLoadingProblemSuggestions: showProblemSuggestionPanel && isLoadingProblemSuggestions,
    userSuggestions: visibleUserSuggestions,
    problemSuggestions: visibleProblemSuggestions,
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
      setUsernameDraft((draft) => ({ ...draft, query: usernameQueryParam, focused }))
    },
    setIsProblemFilterFocused: (focused: boolean) => {
      setProblemDraft((draft) => ({ ...draft, query: problemQueryParam, focused }))
    },
    setIsUserSuggestionEnabled: (checked: boolean) => {
      setIsUserSuggestionEnabled(checked)
      setUsernameDraft((draft) => ({ ...draft, query: usernameQueryParam, focused: checked }))
      if (!checked) {
        setUserSuggestions([])
      }
    },
    setIsProblemSuggestionEnabled: (checked: boolean) => {
      setIsProblemSuggestionEnabled(checked)
      setProblemDraft((draft) => ({ ...draft, query: problemQueryParam, focused: checked }))
      if (!checked) {
        setProblemSuggestions([])
      }
    },
    selectUsernameSuggestion: (username: string) => {
      setUsernameDraft({ query: usernameQueryParam, value: username, selected: username, focused: false })
    },
    selectProblemSuggestion: (slug: string) => {
      setProblemDraft({ query: problemQueryParam, value: slug, focused: false })
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
