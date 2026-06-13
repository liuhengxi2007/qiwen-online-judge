import { useReducer, type KeyboardEvent } from 'react'
import { useSearchParams } from 'react-router-dom'

import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { isSubmissionSort, type SubmissionSort } from '@/objects/submission/request/SubmissionSort'
import {
  isSubmissionSortDirection,
  type SubmissionSortDirection,
} from '@/objects/submission/request/SubmissionSortDirection'
import {
  isSubmissionVerdictFilter,
  type SubmissionVerdictFilter,
} from '@/objects/submission/request/SubmissionVerdictFilter'
import { useSubmissionListQuery } from './useSubmissionListQuery'
import { useSubmissionSuggestions } from './useSubmissionSuggestions'
import {
  buildSubmissionListRequest,
  defaultSortDirection,
  submissionSortValues,
  verdictFilterValues,
} from '@/pages/objects/SubmissionListForm'
import {
  createSubmissionPageState,
  submissionPageReducer,
} from '@/pages/objects/SubmissionPageState'
import {
  buildPageNumbers,
  calculateTotalPages,
  parsePositivePage,
} from '@/pages/objects/Pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { verdictFilterLabel } from '@/pages/objects/SubmissionDisplay'

/**
 * 提交列表页模型 hook，维护 URL 查询参数、筛选输入草稿、建议列表、排序和分页。
 * fixedProblemSlugFilter 存在时题目过滤由路由固定，contestSlug 存在时查询竞赛内提交。
 */
export function useSubmissionPageModel(fixedProblemSlugFilter?: ProblemSlug, contestSlug?: ContestSlug) {
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
  const submissionQuery = useSubmissionListQuery(request, contestSlug)
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

/**
 * 从 URL 查询参数读取提交排序字段，非法值回退为提交时间排序。
 */
function readSubmissionSort(searchParams: URLSearchParams): SubmissionSort {
  const rawSort = searchParams.get('sort')
  return rawSort && isSubmissionSort(rawSort) ? rawSort : 'submitted'
}

/**
 * 从 URL 查询参数读取排序方向，非法值按当前排序字段的默认方向处理。
 */
function readSubmissionSortDirection(
  searchParams: URLSearchParams,
  activeSort: SubmissionSort,
): SubmissionSortDirection {
  const rawDirection = searchParams.get('direction')
  return rawDirection && isSubmissionSortDirection(rawDirection) ? rawDirection : defaultSortDirection(activeSort)
}

/**
 * 从 URL 查询参数读取 verdict 筛选，非法值回退为全部。
 */
function readSubmissionVerdictFilter(searchParams: URLSearchParams): SubmissionVerdictFilter {
  const rawVerdict = searchParams.get('verdict')
  return rawVerdict && isSubmissionVerdictFilter(rawVerdict) ? rawVerdict : 'all'
}

/**
 * 判断输入框内容是否足以展示键入建议，空白输入不触发建议面板。
 */
function shouldShowTypingSuggestions(value: string): boolean {
  return value.trim().length > 0
}
