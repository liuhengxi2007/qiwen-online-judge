import { useEffect, useState, type KeyboardEvent } from 'react'
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom'
import { Files } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { listProblemSuggestions } from '@/features/problem/api/problem-client'
import type { ProblemSuggestion } from '@/features/problem/domain/problem'
import {
  formatProblemTitleDisplay,
  parseProblemSlug,
  problemSlugValue,
  problemTitleValue,
  type ProblemSlug,
  useProblemTitleDisplayMode,
} from '@/features/problem/domain/problem'
import {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  isSubmissionSort,
  isSubmissionSortDirection,
  isSubmissionVerdictFilter,
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
import { useSubmissionListQuery } from '@/features/submission/hooks/use-submission-list-query'
import { listUserSuggestions } from '@/features/user/api/user-client'
import {
  displayNameValue,
  usernameValue,
  type UserIdentity,
} from '@/features/user/domain/user'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

function submissionOverviewStatus(submission: SubmissionSummary): string {
  return submission.status !== 'completed'
    ? submissionStatusLabel(submission.status)
    : submissionVerdictLabel(submission.verdict)
}

const submissionsPerPage = 10

const verdictFilterValues = [
  'all',
  'pending',
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly SubmissionVerdictFilter[]

const submissionSortValues = [
  'submitted',
  'time',
  'memory',
  'code_length',
] as const satisfies readonly SubmissionSort[]

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

function verdictFilterLabel(verdict: SubmissionVerdictFilter, allVerdictsLabel: string): string {
  if (verdict === 'all') {
    return allVerdictsLabel
  }

  if (verdict === 'pending') {
    return submissionVerdictLabel(null)
  }

  return submissionVerdictLabel(verdict)
}

function buildPageNumbers(currentPage: number, totalPages: number): number[] {
  const firstPage = Math.max(1, currentPage - 2)
  const lastPage = Math.min(totalPages, currentPage + 2)
  const pages: number[] = []
  for (let page = firstPage; page <= lastPage; page += 1) {
    pages.push(page)
  }
  return pages
}

function shouldShowTypingSuggestions(value: string): boolean {
  return value.trim().length > 0
}

function parsePositivePage(value: string | null): number {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}

type SubmissionPageProps = {
  fixedProblemSlugFilter?: ProblemSlug
}

export function SubmissionPage({ fixedProblemSlugFilter }: SubmissionPageProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('submission.pageTitle'))
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const { session: user, navigationIntent } = useSessionGuard()
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
    userQuery: usernameQueryParam || null,
    problemQuery: activeProblemQuery || null,
    verdict: activeVerdictFilter,
    sort: activeSort,
    direction: activeDirection,
    page: currentPage,
    pageSize: submissionsPerPage,
  }
  const submissionQuery = useSubmissionListQuery(request)
  const currentPageSubmissions = submissionQuery.response.items
  const totalPages = Math.max(1, Math.ceil(submissionQuery.response.totalItems / submissionQuery.response.pageSize))
  const pageNumbers = buildPageNumbers(currentPage, totalPages)
  const showUserSuggestionPanel =
    isUserSuggestionEnabled && isUsernameFilterFocused && shouldShowTypingSuggestions(usernameFilterInput)
  const showProblemSuggestionPanel =
    isProblemSuggestionEnabled && isProblemFilterFocused && shouldShowTypingSuggestions(problemFilterInput)

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

  const paginationControls =
    totalPages > 1 ? (
      <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-slate-300 bg-white"
          disabled={currentPage === 1}
          onClick={() => {
            const nextSearchParams = new URLSearchParams(searchParams)
            nextSearchParams.set('page', String(Math.max(1, currentPage - 1)))
            setSearchParams(nextSearchParams)
          }}
        >
          {t('submission.pagination.previous')}
        </Button>
        {pageNumbers.map((page) => (
          <Button
            key={page}
            type="button"
            variant={page === currentPage ? 'default' : 'outline'}
            className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'}
            onClick={() => {
              const nextSearchParams = new URLSearchParams(searchParams)
              nextSearchParams.set('page', String(page))
              setSearchParams(nextSearchParams)
            }}
          >
            {page}
          </Button>
        ))}
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-slate-300 bg-white"
          disabled={currentPage === totalPages}
          onClick={() => {
            const nextSearchParams = new URLSearchParams(searchParams)
            nextSearchParams.set('page', String(Math.min(totalPages, currentPage + 1)))
            setSearchParams(nextSearchParams)
          }}
        >
          {t('submission.pagination.next')}
        </Button>
      </div>
    ) : null

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

  function applyFiltersOnEnter(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') {
      return
    }

    event.preventDefault()
    applyFilters()
  }

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('submission.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        {submissionQuery.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{submissionQuery.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="mb-6 border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
                <Files className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">{t('submission.filter.title')}</CardTitle>
                <CardDescription>{t('submission.filter.description')}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div
              className={`grid gap-4 ${
                hasFixedProblemFilter
                  ? 'lg:grid-cols-[minmax(0,5fr)_minmax(0,4fr)_minmax(0,5fr)]'
                  : 'lg:grid-cols-[minmax(0,5fr)_minmax(0,5fr)_minmax(0,4fr)_minmax(0,5fr)]'
              }`}
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-3">
                  <Label htmlFor="submission-username-filter">{t('common.username')}</Label>
                  <div className="flex items-center gap-2">
                    <Label htmlFor="submission-user-suggestion-toggle" className="text-xs text-slate-500">
                      {t('submission.filter.toggleUserSearch')}
                    </Label>
                    <Switch
                      id="submission-user-suggestion-toggle"
                      checked={isUserSuggestionEnabled}
                      onCheckedChange={(checked) => {
                        setIsUserSuggestionEnabled(checked)
                        setIsUsernameFilterFocused(checked)
                        if (!checked) {
                          setUserSuggestions([])
                        }
                      }}
                    />
                  </div>
                </div>
                <div>
                  <Input
                    id="submission-username-filter"
                    className="min-w-0"
                    value={usernameFilterInput}
                    onChange={(event) => {
                      updateUsernameFilterInput(event.target.value)
                    }}
                    onFocus={() => {
                      setIsUsernameFilterFocused(true)
                    }}
                    onBlur={() => {
                      setIsUsernameFilterFocused(false)
                    }}
                    onKeyDown={applyFiltersOnEnter}
                  />
                </div>
                {showUserSuggestionPanel ? (
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
                    {isLoadingUserSuggestions ? (
                      <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
                    ) : userSuggestions.length === 0 ? (
                      <p className="px-3 py-2 text-sm text-slate-500">{t('common.emptyData')}</p>
                    ) : (
                      userSuggestions.map((suggestion) => (
                        <button
                          key={usernameValue(suggestion.username)}
                          type="button"
                          className="flex w-full flex-col rounded-xl px-3 py-2 text-left text-sm hover:bg-white"
                          onMouseDown={(event) => {
                            event.preventDefault()
                          }}
                          onClick={() => {
                            setUsernameFilterInput(usernameValue(suggestion.username))
                            setSelectedUsernameSuggestion(usernameValue(suggestion.username))
                            setIsUsernameFilterFocused(false)
                          }}
                        >
                          <span className="font-medium text-slate-900">{displayNameValue(suggestion.displayName)}</span>
                          <span className="text-slate-500">{usernameValue(suggestion.username)}</span>
                        </button>
                      ))
                    )}
                  </div>
                ) : null}
              </div>

              {hasFixedProblemFilter ? null : (
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-3">
                    <Label htmlFor="submission-problem-filter">{t('submission.filter.problemSlug')}</Label>
                    <div className="flex items-center gap-2">
                      <Label htmlFor="submission-problem-suggestion-toggle" className="text-xs text-slate-500">
                        {t('submission.filter.toggleProblemSearch')}
                      </Label>
                      <Switch
                        id="submission-problem-suggestion-toggle"
                        checked={isProblemSuggestionEnabled}
                        onCheckedChange={(checked) => {
                          setIsProblemSuggestionEnabled(checked)
                          setIsProblemFilterFocused(checked)
                          if (!checked) {
                            setProblemSuggestions([])
                          }
                        }}
                      />
                    </div>
                  </div>
                  <div>
                    <Input
                      id="submission-problem-filter"
                      className="min-w-0"
                      value={problemFilterInput}
                      onChange={(event) => {
                        updateProblemFilterInput(event.target.value)
                      }}
                      onFocus={() => {
                        setIsProblemFilterFocused(true)
                      }}
                      onBlur={() => {
                        setIsProblemFilterFocused(false)
                      }}
                      onKeyDown={applyFiltersOnEnter}
                    />
                  </div>
                  {showProblemSuggestionPanel ? (
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
                      {isLoadingProblemSuggestions ? (
                        <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
                      ) : problemSuggestions.length === 0 ? (
                        <p className="px-3 py-2 text-sm text-slate-500">{t('common.emptyData')}</p>
                      ) : (
                        problemSuggestions.map((suggestion) => (
                          <button
                            key={problemSlugValue(suggestion.slug)}
                            type="button"
                            className="flex w-full flex-col rounded-xl px-3 py-2 text-left text-sm hover:bg-white"
                            onMouseDown={(event) => {
                              event.preventDefault()
                            }}
                            onClick={() => {
                              setProblemFilterInput(problemSlugValue(suggestion.slug))
                              setIsProblemFilterFocused(false)
                            }}
                          >
                            <span className="font-medium text-slate-900">{problemTitleValue(suggestion.title)}</span>
                            <span className="text-slate-500">{problemSlugValue(suggestion.slug)}</span>
                          </button>
                        ))
                      )}
                    </div>
                  ) : null}
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="submission-verdict-filter">{t('submission.filter.verdict')}</Label>
                <Select
                  value={activeVerdictFilter}
                  onValueChange={(value) => {
                    if (isSubmissionVerdictFilter(value)) {
                      updateSearchFilter('verdict', value === 'all' ? null : value)
                    }
                  }}
                >
                  <SelectTrigger id="submission-verdict-filter" className="min-w-32 rounded-2xl border-slate-300 bg-white">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {verdictFilterValues.map((verdict) => (
                      <SelectItem key={verdict} value={verdict}>
                        {verdictFilterLabel(verdict, t('submission.filter.allVerdicts'))}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="submission-sort">{t('submission.filter.sort')}</Label>
                <div className="flex flex-wrap gap-2">
                  <Select
                    value={activeSort}
                    onValueChange={(value) => {
                      if (isSubmissionSort(value)) {
                        const nextSearchParams = new URLSearchParams(searchParams)
                        if (value === 'submitted') {
                          nextSearchParams.delete('sort')
                        } else {
                          nextSearchParams.set('sort', value)
                        }
                        const nextDirection = defaultSortDirection(value)
                        if (nextDirection === defaultSortDirection(value)) {
                          nextSearchParams.delete('direction')
                        } else {
                          nextSearchParams.set('direction', nextDirection)
                        }
                        nextSearchParams.delete('page')
                        setSearchParams(nextSearchParams)
                      }
                    }}
                  >
                    <SelectTrigger id="submission-sort" className="min-w-40 flex-1 rounded-2xl border-slate-300 bg-white">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {submissionSortValues.map((sort) => (
                        <SelectItem key={sort} value={sort}>
                          {t(`submission.sort.${sort}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button
                    type="button"
                    variant="outline"
                    className="shrink-0 rounded-2xl border-slate-300 bg-white"
                    onClick={() => {
                      const nextDirection: SubmissionSortDirection = activeDirection === 'asc' ? 'desc' : 'asc'
                      updateSearchFilter('direction', nextDirection === defaultSortDirection(activeSort) ? null : nextDirection)
                    }}
                  >
                    {activeDirection === 'asc' ? t('submission.sort.ascending') : t('submission.sort.descending')}
                  </Button>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                onClick={() => {
                  applyFilters()
                }}
              >
                {t('submission.filter.apply')}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="rounded-2xl border-slate-300 bg-white"
                onClick={() => {
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
                }}
              >
                {t('submission.filter.clear')}
              </Button>
            </div>

            {usernameQueryParam ? (
              <p className="text-sm text-slate-600">
                {t('submission.filter.showingUser', {
                  query: usernameQueryParam,
                })}
              </p>
            ) : (
              <p className="text-sm text-slate-600">{t('submission.filter.showingAll')}</p>
            )}
            <p className="text-sm text-slate-600">
              {t('submission.filter.activeSummary', {
                problem: activeProblemQuery || t('submission.filter.anyProblem'),
                verdict: verdictFilterLabel(activeVerdictFilter, t('submission.filter.allVerdicts')),
              })}
            </p>
          </CardContent>
        </Card>

        {!submissionQuery.isLoading && currentPageSubmissions.length > 0 ? (
          <div className="mb-6">{paginationControls}</div>
        ) : null}

        {submissionQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('submission.list.loading')}</CardContent>
          </Card>
        ) : currentPageSubmissions.length === 0 ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('submission.list.empty')}</CardContent>
          </Card>
        ) : (
          <div className="space-y-4">
            {currentPageSubmissions.map((submission) => (
              <Card
                key={submissionIdValue(submission.id)}
                className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]"
              >
                <CardContent className="py-3.5">
                  <dl className="grid gap-2 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-[minmax(0,2fr)_minmax(0,6fr)_minmax(0,6fr)_minmax(0,2fr)_minmax(0,4fr)_minmax(0,6fr)_minmax(0,3fr)_minmax(0,3fr)_minmax(0,3fr)]">
                    <div>
                      <dt className="text-slate-500">{t('submission.list.id')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        {submission.canViewDetail || usernameValue(submission.submitter.username) === usernameValue(user.username) ? (
                          <Link
                            className="-mx-2 block min-h-[1.625rem] w-full rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline"
                            to={`/submissions/${submissionIdValue(submission.id)}`}
                          >
                            {submissionIdValue(submission.id)}
                          </Link>
                        ) : (
                          <span className="block min-h-[1.625rem] w-full px-2 py-1">{submissionIdValue(submission.id)}</span>
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.problem')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <Link
                          className="-mx-2 block min-h-[1.625rem] w-full rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline"
                          to={`/problems/${problemSlugValue(submission.problemSlug)}`}
                        >
                          {formatProblemTitleDisplay(submission.problemTitle, submission.problemSlug, problemTitleDisplayMode)}
                        </Link>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.submitter')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <UserProfileLink
                          className="block"
                          linkClassName="-mx-2 block min-h-[1.625rem] w-full rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline"
                          user={submission.submitter}
                        />
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.language')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <span className="block min-h-[1.625rem] w-full py-1">{submissionLanguageLabel(submission.language)}</span>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('common.verdict')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <span className="block min-h-[1.625rem] w-full py-1">{submissionOverviewStatus(submission)}</span>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('common.submittedAt')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <span className="block min-h-[1.625rem] w-full py-1">{new Date(submission.submittedAt).toLocaleString()}</span>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.timeUsed')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <span className="block min-h-[1.625rem] w-full py-1">{formatOptionalDurationMs(submission.timeUsedMs)}</span>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.spaceUsed')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <span className="block min-h-[1.625rem] w-full py-1">{formatOptionalMemoryKb(submission.memoryUsedKb)}</span>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.codeLength')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        <span className="block min-h-[1.625rem] w-full py-1">{formatCodeLength(submission.codeLength)}</span>
                      </dd>
                    </div>
                  </dl>
                </CardContent>
              </Card>
            ))}
            {paginationControls}
          </div>
        )}
      </section>
    </main>
  )
}

export function ProblemSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  return <SubmissionPage fixedProblemSlugFilter={slugResult.value} />
}
