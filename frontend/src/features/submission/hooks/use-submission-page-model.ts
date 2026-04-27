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

function buildPageNumbers(currentPage: number, totalPages: number): number[] {
  const firstPage = Math.max(1, currentPage - 2)
  const lastPage = Math.min(totalPages, currentPage + 2)
  return Array.from({ length: lastPage - firstPage + 1 }, (_, index) => firstPage + index)
}

function shouldShowTypingSuggestions(value: string): boolean {
  return value.trim().length > 0
}

function parsePositivePage(value: string | null): number {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
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
  const [usernameFilterInput, setUsernameFilterInput] = useState('')
  const [problemFilterInput, setProblemFilterInput] = useState('')
  const [isUsernameFilterFocused, setIsUsernameFilterFocused] = useState(false)
  const [isProblemFilterFocused, setIsProblemFilterFocused] = useState(false)
  const [isUserSuggestionEnabled, setIsUserSuggestionEnabled] = useState(false)
  const [isProblemSuggestionEnabled, setIsProblemSuggestionEnabled] = useState(false)
  const [isLoadingUserSuggestions, setIsLoadingUserSuggestions] = useState(false)
  const [isLoadingProblemSuggestions, setIsLoadingProblemSuggestions] = useState(false)
  const [selectedUsernameSuggestion, setSelectedUsernameSuggestion] = useState<string | null>(null)
  const [userSuggestions, setUserSuggestions] = useState<UserIdentity[]>([])
  const [problemSuggestions, setProblemSuggestions] = useState<ProblemSuggestion[]>([])
  const hasFixedProblemFilter = fixedProblemSlugFilter !== undefined
  const activeProblemQuery = hasFixedProblemFilter ? problemSlugValue(fixedProblemSlugFilter) : problemQueryParam
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
  const totalPages = Math.max(1, Math.ceil(submissionQuery.response.totalItems / submissionQuery.response.pageSize))
  const pageNumbers = buildPageNumbers(currentPage, totalPages)
  const showUserSuggestionPanel =
    isUserSuggestionEnabled && isUsernameFilterFocused && shouldShowTypingSuggestions(usernameFilterInput)
  const showProblemSuggestionPanel =
    isProblemSuggestionEnabled && isProblemFilterFocused && shouldShowTypingSuggestions(problemFilterInput)

  useEffect(() => {
    setUsernameFilterInput(usernameQueryParam)
    setSelectedUsernameSuggestion(usernameQueryParam || null)
    setIsUsernameFilterFocused(false)
  }, [usernameQueryParam])

  useEffect(() => {
    if (!hasFixedProblemFilter) {
      setProblemFilterInput(activeProblemQuery)
    }
    setIsProblemFilterFocused(false)
  }, [activeProblemQuery, hasFixedProblemFilter])

  useEffect(() => {
    if (currentPage > totalPages) {
      const nextSearchParams = new URLSearchParams(searchParams)
      if (totalPages <= 1) {
        nextSearchParams.delete('page')
      } else {
        nextSearchParams.set('page', String(totalPages))
      }
      setSearchParams(nextSearchParams)
    }
  }, [currentPage, searchParams, setSearchParams, totalPages])

  useEffect(() => {
    if (!isUserSuggestionEnabled || !isUsernameFilterFocused || !shouldShowTypingSuggestions(usernameFilterInput)) {
      setUserSuggestions([])
      setIsLoadingUserSuggestions(false)
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
      setIsLoadingUserSuggestions(false)
      window.clearTimeout(timeoutId)
    }
  }, [isUserSuggestionEnabled, isUsernameFilterFocused, usernameFilterInput])

  useEffect(() => {
    if (!isProblemSuggestionEnabled || !isProblemFilterFocused || !shouldShowTypingSuggestions(problemFilterInput) || hasFixedProblemFilter) {
      setProblemSuggestions([])
      setIsLoadingProblemSuggestions(false)
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
      setIsLoadingProblemSuggestions(false)
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
    setUsernameFilterInput(value)
    setSelectedUsernameSuggestion(null)
    setIsUsernameFilterFocused(true)
  }

  function updateProblemFilterInput(value: string) {
    setProblemFilterInput(value)
    setIsProblemFilterFocused(true)
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
    setIsUsernameFilterFocused(false)
    setIsProblemFilterFocused(false)
    setSearchParams(nextSearchParams)
  }

  function clearFilters() {
    setUsernameFilterInput('')
    setSelectedUsernameSuggestion(null)
    setProblemFilterInput('')
    setIsUsernameFilterFocused(false)
    setIsProblemFilterFocused(false)
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
    setIsUsernameFilterFocused,
    setIsProblemFilterFocused,
    setIsUserSuggestionEnabled: (checked: boolean) => {
      setIsUserSuggestionEnabled(checked)
      setIsUsernameFilterFocused(checked)
      if (!checked) {
        setUserSuggestions([])
      }
    },
    setIsProblemSuggestionEnabled: (checked: boolean) => {
      setIsProblemSuggestionEnabled(checked)
      setIsProblemFilterFocused(checked)
      if (!checked) {
        setProblemSuggestions([])
      }
    },
    selectUsernameSuggestion: (username: string) => {
      setUsernameFilterInput(username)
      setSelectedUsernameSuggestion(username)
      setIsUsernameFilterFocused(false)
    },
    selectProblemSuggestion: (slug: string) => {
      setProblemFilterInput(slug)
      setIsProblemFilterFocused(false)
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
