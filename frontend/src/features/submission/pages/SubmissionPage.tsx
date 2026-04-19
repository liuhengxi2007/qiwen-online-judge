import { useEffect, useState, type KeyboardEvent } from 'react'
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, Files } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  displayNameValue,
  parseUsername,
  usernameValue,
  type Username,
} from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  submissionIdValue,
  submissionLanguageLabel,
  submissionStatusLabel,
  submissionVerdictLabel,
  type SubmissionSummary,
  type SubmissionVerdict,
} from '@/features/submission/domain/submission'
import { parseProblemSlug, problemSlugValue, problemTitleValue, type ProblemSlug } from '@/features/problem/domain/problem'
import { useSubmissionListQuery } from '@/features/submission/hooks/use-submission-list-query'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

function formatOptionalDurationMs(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${value} ms`
}

function formatOptionalMemoryKb(value: number | null): string {
  if (value === null) {
    return '--'
  }

  if (value < 1024) {
    return `${value} KB`
  }

  return `${(value / 1024).toFixed(1)} MB`
}

function formatCodeLength(value: number): string {
  return `${value} B`
}

function submissionOverviewStatus(submission: SubmissionSummary): string {
  return submission.status !== 'completed'
    ? submissionStatusLabel(submission.status)
    : submissionVerdictLabel(submission.verdict)
}

type VerdictFilter = 'all' | 'pending' | SubmissionVerdict
type SubmissionSort = 'submitted' | 'time' | 'memory' | 'code_length'
type SortDirection = 'asc' | 'desc'

const submissionsPerPage = 10
const searchSuggestionLimit = 5

const verdictFilterValues = [
  'all',
  'pending',
  'accepted',
  'wrong_answer',
  'compile_error',
  'runtime_error',
  'time_limit_exceeded',
  'system_error',
] as const satisfies readonly VerdictFilter[]

const submissionSortValues = [
  'time',
  'memory',
  'code_length',
  'submitted',
] as const satisfies readonly SubmissionSort[]

function isVerdictFilter(value: string): value is VerdictFilter {
  return (verdictFilterValues as readonly string[]).includes(value)
}

function isSubmissionSort(value: string): value is SubmissionSort {
  return (submissionSortValues as readonly string[]).includes(value)
}

type UserFilterSuggestion = {
  username: Username
  displayName: string
}

type ProblemFilterSuggestion = {
  slug: string
  title: string
}

function normalizeSearchText(value: string): string {
  return value.trim().toLocaleLowerCase()
}

function fuzzyScore(candidate: string, input: string): number {
  const normalizedCandidate = normalizeSearchText(candidate)
  const normalizedInput = normalizeSearchText(input)

  if (!normalizedInput) {
    return 1
  }

  if (normalizedCandidate === normalizedInput) {
    return 1000
  }

  if (normalizedCandidate.startsWith(normalizedInput)) {
    return 800 - normalizedCandidate.length
  }

  const index = normalizedCandidate.indexOf(normalizedInput)
  if (index >= 0) {
    return 600 - index - normalizedCandidate.length
  }

  let candidateIndex = 0
  let matched = 0
  for (const character of normalizedInput) {
    const foundIndex = normalizedCandidate.indexOf(character, candidateIndex)
    if (foundIndex < 0) {
      return 0
    }
    matched += 1
    candidateIndex = foundIndex + 1
  }

  return 300 + matched - normalizedCandidate.length
}

function bestFuzzyScore(candidates: string[], input: string): number {
  return Math.max(...candidates.map((candidate) => fuzzyScore(candidate, input)))
}

function buildUserSuggestions(
  submissions: SubmissionSummary[],
  input: string,
): UserFilterSuggestion[] {
  const normalizedInput = normalizeSearchText(input)
  if (!normalizedInput) {
    return []
  }

  const suggestions = new Map<string, UserFilterSuggestion>()
  for (const submission of submissions) {
    const username = usernameValue(submission.submitter.username)
    if (!suggestions.has(username)) {
      suggestions.set(username, {
        username: submission.submitter.username,
        displayName: displayNameValue(submission.submitter.displayName),
      })
    }
  }

  return [...suggestions.values()]
    .map((suggestion) => ({
      suggestion,
      score: bestFuzzyScore([usernameValue(suggestion.username), suggestion.displayName], input),
    }))
    .filter(({ score }) => score > 0)
    .sort((left, right) => right.score - left.score || usernameValue(left.suggestion.username).localeCompare(usernameValue(right.suggestion.username)))
    .slice(0, searchSuggestionLimit)
    .map(({ suggestion }) => suggestion)
}

function buildProblemSuggestions(
  submissions: SubmissionSummary[],
  input: string,
): ProblemFilterSuggestion[] {
  const normalizedInput = normalizeSearchText(input)
  if (!normalizedInput) {
    return []
  }

  const suggestions = new Map<string, ProblemFilterSuggestion>()
  for (const submission of submissions) {
    const slug = problemSlugValue(submission.problemSlug)
    if (!suggestions.has(slug)) {
      suggestions.set(slug, {
        slug,
        title: problemTitleValue(submission.problemTitle),
      })
    }
  }

  return [...suggestions.values()]
    .map((suggestion) => ({
      suggestion,
      score: bestFuzzyScore([suggestion.slug, suggestion.title], input),
    }))
    .filter(({ score }) => score > 0)
    .sort((left, right) => right.score - left.score || left.suggestion.slug.localeCompare(right.suggestion.slug))
    .slice(0, searchSuggestionLimit)
    .map(({ suggestion }) => suggestion)
}

function matchesUsernameFilter(submission: SubmissionSummary, username: Username | null): boolean {
  return username === null || usernameValue(submission.submitter.username) === usernameValue(username)
}

function matchesProblemSlugTextFilter(submission: SubmissionSummary, problemSlug: string): boolean {
  const normalizedProblemSlug = normalizeSearchText(problemSlug)
  return !normalizedProblemSlug || normalizeSearchText(problemSlugValue(submission.problemSlug)) === normalizedProblemSlug
}

function matchesProblemSlugFilter(submission: SubmissionSummary, problemSlug: ProblemSlug | null): boolean {
  return problemSlug === null || problemSlugValue(submission.problemSlug) === problemSlugValue(problemSlug)
}

function matchesVerdictFilter(submission: SubmissionSummary, verdict: VerdictFilter): boolean {
  if (verdict === 'all') {
    return true
  }

  if (verdict === 'pending') {
    return submission.verdict === null
  }

  return submission.verdict === verdict
}

function compareNullableNumber(left: number | null, right: number | null): number {
  if (left === null && right === null) {
    return 0
  }

  if (left === null) {
    return 1
  }

  if (right === null) {
    return -1
  }

  return left - right
}

function compareSubmissions(left: SubmissionSummary, right: SubmissionSummary, sort: SubmissionSort, direction: SortDirection): number {
  const directionMultiplier = direction === 'asc' ? 1 : -1

  switch (sort) {
    case 'submitted':
      return (
        directionMultiplier * (new Date(left.submittedAt).getTime() - new Date(right.submittedAt).getTime()) ||
        directionMultiplier * (submissionIdValue(left.id) - submissionIdValue(right.id))
      )
    case 'time':
      return directionMultiplier * compareNullableNumber(left.timeUsedMs, right.timeUsedMs) || compareSubmissions(left, right, 'submitted', 'desc')
    case 'memory':
      return directionMultiplier * compareNullableNumber(left.memoryUsedKb, right.memoryUsedKb) || compareSubmissions(left, right, 'submitted', 'desc')
    case 'code_length':
      return directionMultiplier * (left.codeLength - right.codeLength) || compareSubmissions(left, right, 'submitted', 'desc')
  }
}

function verdictFilterLabel(verdict: VerdictFilter, allVerdictsLabel: string): string {
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

type SubmissionPageProps = {
  fixedProblemSlugFilter?: ProblemSlug
}

export function SubmissionPage({ fixedProblemSlugFilter }: SubmissionPageProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('submission.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const usernameQueryParam = searchParams.get('username')?.trim() ?? ''
  const usernameQueryResult = usernameQueryParam ? parseUsername(usernameQueryParam) : null
  const queryUsernameFilter = usernameQueryResult?.ok ? usernameQueryResult.value : null
  const [usernameFilterInput, setUsernameFilterInput] = useState('')
  const [problemFilterInput, setProblemFilterInput] = useState('')
  const [activeProblemTitleFilter, setActiveProblemTitleFilter] = useState('')
  const [activeVerdictFilter, setActiveVerdictFilter] = useState<VerdictFilter>('all')
  const [activeSort, setActiveSort] = useState<SubmissionSort>('submitted')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [currentPage, setCurrentPage] = useState(1)
  const [filterErrorMessage, setFilterErrorMessage] = useState('')
  const [isUsernameFilterFocused, setIsUsernameFilterFocused] = useState(false)
  const [isProblemFilterFocused, setIsProblemFilterFocused] = useState(false)
  const [selectedUsernameSuggestion, setSelectedUsernameSuggestion] = useState<Username | null>(null)
  const submissionQuery = useSubmissionListQuery(queryUsernameFilter)
  const userSuggestions = buildUserSuggestions(submissionQuery.submissions, usernameFilterInput)
  const problemSuggestions = buildProblemSuggestions(submissionQuery.submissions, problemFilterInput)
  const showUserSuggestions = isUsernameFilterFocused && shouldShowTypingSuggestions(usernameFilterInput) && userSuggestions.length > 0
  const showProblemSuggestions = isProblemFilterFocused && shouldShowTypingSuggestions(problemFilterInput) && problemSuggestions.length > 0
  const effectiveUsernameFilter = queryUsernameFilter
  const hasFixedProblemFilter = fixedProblemSlugFilter !== undefined
  const visibleSubmissions = submissionQuery.submissions.filter(
    (submission) =>
      matchesUsernameFilter(submission, effectiveUsernameFilter) &&
      matchesProblemSlugFilter(submission, fixedProblemSlugFilter ?? null) &&
      (hasFixedProblemFilter || matchesProblemSlugTextFilter(submission, activeProblemTitleFilter)) &&
      matchesVerdictFilter(submission, activeVerdictFilter),
  )
  const sortedSubmissions = [...visibleSubmissions].sort((left, right) => compareSubmissions(left, right, activeSort, sortDirection))
  const totalPages = Math.max(1, Math.ceil(sortedSubmissions.length / submissionsPerPage))
  const currentPageSubmissions = sortedSubmissions.slice((currentPage - 1) * submissionsPerPage, currentPage * submissionsPerPage)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)
  const paginationControls =
    totalPages > 1 ? (
      <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-slate-300 bg-white"
          disabled={currentPage === 1}
          onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
        >
          {t('submission.pagination.previous')}
        </Button>
        {pageNumbers.map((page) => (
          <Button
            key={page}
            type="button"
            variant={page === currentPage ? 'default' : 'outline'}
            className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'}
            onClick={() => setCurrentPage(page)}
          >
            {page}
          </Button>
        ))}
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-slate-300 bg-white"
          disabled={currentPage === totalPages}
          onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
        >
          {t('submission.pagination.next')}
        </Button>
      </div>
    ) : null

  useEffect(() => {
    setCurrentPage(1)
  }, [queryUsernameFilter, activeProblemTitleFilter, activeVerdictFilter, activeSort, sortDirection, fixedProblemSlugFilter])

  useEffect(() => {
    setUsernameFilterInput(queryUsernameFilter ? usernameValue(queryUsernameFilter) : '')
    setSelectedUsernameSuggestion(queryUsernameFilter)
    setIsUsernameFilterFocused(false)
  }, [queryUsernameFilter])

  function updateUsernameFilterInput(value: string) {
    setUsernameFilterInput(value)
    setSelectedUsernameSuggestion(null)
    setIsUsernameFilterFocused(true)
    setFilterErrorMessage('')
  }

  function updateProblemFilterInput(value: string) {
    setProblemFilterInput(value)
    setIsProblemFilterFocused(true)
    setFilterErrorMessage('')
  }

  function applyFilters() {
    const trimmedUsernameInput = usernameFilterInput.trim()
    const nextSearchParams = new URLSearchParams(searchParams)
    if (!trimmedUsernameInput) {
      nextSearchParams.delete('username')
    } else {
      const nextUsername =
        selectedUsernameSuggestion !== null
          ? selectedUsernameSuggestion
          : (() => {
              const usernameResult = parseUsername(trimmedUsernameInput)
              if (!usernameResult.ok) {
                setFilterErrorMessage(usernameResult.error)
                return null
              }

              return usernameResult.value
            })()

      if (nextUsername === null) {
        return
      }

      nextSearchParams.set('username', usernameValue(nextUsername))
    }

    setActiveProblemTitleFilter(problemFilterInput.trim())
    setIsUsernameFilterFocused(false)
    setIsProblemFilterFocused(false)
    setFilterErrorMessage('')
    setSearchParams(nextSearchParams)
  }

  function applyFiltersOnEnter(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') {
      return
    }

    event.preventDefault()
    applyFilters()
  }

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages)
    }
  }, [currentPage, totalPages])

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
            <CardTitle className="text-xl text-slate-950">{t('submission.filter.title')}</CardTitle>
            <CardDescription>{t('submission.filter.description')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className={`grid gap-4 ${hasFixedProblemFilter ? 'lg:grid-cols-3' : 'lg:grid-cols-4'}`}>
              <div className="space-y-2">
                <Label htmlFor="submission-username-filter">{t('common.username')}</Label>
                <div>
                  <Input
                    id="submission-username-filter"
                    className="min-w-0"
                    value={usernameFilterInput}
                    placeholder={t('submission.filter.usernamePlaceholder')}
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
                {showUserSuggestions ? (
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
                    {userSuggestions.map((suggestion) => (
                      <button
                        key={usernameValue(suggestion.username)}
                        type="button"
                        className="flex w-full flex-col rounded-xl px-3 py-2 text-left text-sm hover:bg-white"
                        onMouseDown={(event) => {
                          event.preventDefault()
                        }}
                        onClick={() => {
                          setUsernameFilterInput(usernameValue(suggestion.username))
                          setSelectedUsernameSuggestion(suggestion.username)
                          setIsUsernameFilterFocused(false)
                          setFilterErrorMessage('')
                        }}
                      >
                        <span className="font-medium text-slate-900">{suggestion.displayName}</span>
                        <span className="text-slate-500">{usernameValue(suggestion.username)}</span>
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>

              {hasFixedProblemFilter ? null : (
                <div className="space-y-2">
                  <Label htmlFor="submission-problem-filter">{t('submission.filter.problemSlug')}</Label>
                  <div>
                    <Input
                      id="submission-problem-filter"
                      className="min-w-0"
                      value={problemFilterInput}
                      placeholder={t('submission.filter.problemSlugPlaceholder')}
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
                  {showProblemSuggestions ? (
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
                      {problemSuggestions.map((suggestion) => (
                        <button
                          key={suggestion.slug}
                          type="button"
                          className="flex w-full flex-col rounded-xl px-3 py-2 text-left text-sm hover:bg-white"
                          onMouseDown={(event) => {
                            event.preventDefault()
                          }}
                          onClick={() => {
                            setProblemFilterInput(suggestion.slug)
                            setIsProblemFilterFocused(false)
                            setFilterErrorMessage('')
                          }}
                        >
                          <span className="font-medium text-slate-900">{suggestion.title}</span>
                          <span className="text-slate-500">{suggestion.slug}</span>
                        </button>
                      ))}
                    </div>
                  ) : null}
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="submission-verdict-filter">{t('submission.filter.verdict')}</Label>
                <Select
                  value={activeVerdictFilter}
                  onValueChange={(value) => {
                    if (isVerdictFilter(value)) {
                      setActiveVerdictFilter(value)
                    }
                  }}
                >
                  <SelectTrigger id="submission-verdict-filter" className="rounded-2xl border-slate-300 bg-white">
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
                <div className="flex gap-2">
                  <Select
                    value={activeSort}
                    onValueChange={(value) => {
                      if (isSubmissionSort(value)) {
                        setActiveSort(value)
                      }
                    }}
                  >
                    <SelectTrigger id="submission-sort" className="rounded-2xl border-slate-300 bg-white">
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
                    onClick={() => setSortDirection((currentDirection) => (currentDirection === 'asc' ? 'desc' : 'asc'))}
                  >
                    {sortDirection === 'asc' ? t('submission.sort.ascending') : t('submission.sort.descending')}
                  </Button>
                </div>
              </div>
            </div>

            {filterErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{filterErrorMessage}</AlertDescription>
              </Alert>
            ) : null}

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
                  if (!hasFixedProblemFilter) {
                    setActiveProblemTitleFilter('')
                  }
                  setActiveVerdictFilter('all')
                  setActiveSort('submitted')
                  setSortDirection('desc')
                  setFilterErrorMessage('')
                  setSearchParams(nextSearchParams)
                }}
              >
                {t('submission.filter.clear')}
              </Button>
            </div>

            {effectiveUsernameFilter ? (
              <p className="text-sm text-slate-600">
                {t('submission.filter.showingUser', {
                  username: usernameValue(effectiveUsernameFilter),
                })}
              </p>
            ) : (
              <p className="text-sm text-slate-600">{t('submission.filter.showingAll')}</p>
            )}
            <p className="text-sm text-slate-600">
              {t('submission.filter.activeSummary', {
                problem: hasFixedProblemFilter
                  ? problemSlugValue(fixedProblemSlugFilter)
                  : activeProblemTitleFilter || t('submission.filter.anyProblem'),
                verdict: verdictFilterLabel(activeVerdictFilter, t('submission.filter.allVerdicts')),
              })}
            </p>
          </CardContent>
        </Card>

        {!submissionQuery.isLoading && visibleSubmissions.length > 0 ? (
          <div className="mb-6">{paginationControls}</div>
        ) : null}

        {submissionQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('submission.list.loading')}</CardContent>
          </Card>
        ) : visibleSubmissions.length === 0 ? (
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
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
                      <Files className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">
                        Submission #{submissionIdValue(submission.id)}
                      </CardTitle>
                      <CardDescription className="mt-2 text-sm font-medium text-slate-700">
                        {submissionIdValue(submission.id)}
                      </CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
                  <dl className="grid gap-4 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-8">
                    <div>
                      <dt className="text-slate-500">{t('submission.list.problem')}</dt>
                      <dd className="mt-1">
                        <Link
                          className="font-medium text-slate-900 hover:underline"
                          to={`/problems/${problemSlugValue(submission.problemSlug)}`}
                        >
                          {problemTitleValue(submission.problemTitle)}
                        </Link>
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.submitter')}</dt>
                      <dd className="mt-1">
                        <UserProfileLink user={submission.submitter} />
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('common.languageLabel')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        {submissionLanguageLabel(submission.language)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('common.verdict')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">{submissionOverviewStatus(submission)}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('common.submittedAt')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        {new Date(submission.submittedAt).toLocaleString()}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.timeUsed')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">{formatOptionalDurationMs(submission.timeUsedMs)}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.spaceUsed')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        {formatOptionalMemoryKb(submission.memoryUsedKb)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">{t('submission.list.codeLength')}</dt>
                      <dd className="mt-1 font-medium text-slate-900">{formatCodeLength(submission.codeLength)}</dd>
                    </div>
                  </dl>

                  <Button asChild className="rounded-2xl bg-indigo-300 text-indigo-950 hover:bg-indigo-400">
                    <Link to={`/submissions/${submissionIdValue(submission.id)}`}>
                      {t('submission.list.viewSource')}
                      <ArrowRight className="size-4" />
                    </Link>
                  </Button>
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
