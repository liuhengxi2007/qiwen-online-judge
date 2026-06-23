import type { ReactNode } from 'react'
import { useEffect, useReducer } from 'react'
import { Link, Navigate } from 'react-router-dom'

import { CardContent } from '@/components/ui/card'
import { ListHacks } from '@/apis/hack/ListHacks'
import type { HackSummary } from '@/objects/hack/response/HackSummary'
import { hackIdValue } from '@/objects/hack/HackId'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { HackCard, HackErrorAlert } from '@/pages/components/HackCard'
import { PageShell } from '@/pages/components/PageShell'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { hackStatusLabel } from '@/pages/objects/HackDisplay'
import { formatOptionalScore } from '@/pages/objects/SubmissionDisplay'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * Hack 列表状态，保存列表、加载标记和错误消息。
 */
type HackListState = {
  hacks: HackSummary[]
  page: number
  pageSize: number
  totalItems: number
  isLoading: boolean
  errorMessage: string
}

/**
 * Hack 列表 reducer 动作，覆盖加载成功和失败。
 */
type HackListAction =
  | { type: 'loaded'; hacks: HackSummary[]; page: number; pageSize: number; totalItems: number }
  | { type: 'failed'; message: string }
  | { type: 'page'; page: number }

/**
 * Hack 列表 reducer；纯函数维护加载状态和列表数据。
 */
function reducer(state: HackListState, action: HackListAction): HackListState {
  switch (action.type) {
    case 'loaded':
      return { ...state, hacks: action.hacks, page: action.page, pageSize: action.pageSize, totalItems: action.totalItems, isLoading: false, errorMessage: '' }
    case 'failed':
      return { ...state, isLoading: false, errorMessage: action.message }
    case 'page':
      return { ...state, page: action.page, isLoading: true }
  }
}

/**
 * Hack 列表页，加载最近 Hack 记录并展示状态、分数变化和目标提交。
 */
export function HackPage() {
  const { t } = useI18n()
  usePageTitle(t('hack.list.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [state, dispatch] = useReducer(reducer, { hacks: [], page: 1, pageSize: 20, totalItems: 0, isLoading: true, errorMessage: '' })

  useEffect(() => {
    let cancelled = false
    void sendAPI(new ListHacks(state.page, state.pageSize))
      .then((response) => {
        if (!cancelled) {
          dispatch({ type: 'loaded', hacks: response.items, page: response.page, pageSize: response.pageSize, totalItems: response.totalItems })
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          dispatch({ type: 'failed', message: isHttpClientError(error) ? error.message : t('hack.list.loadFailed') })
        }
      })
    return () => {
      cancelled = true
    }
  }, [state.page, state.pageSize, t])

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const totalPages = Math.max(1, Math.ceil(state.totalItems / state.pageSize))
  const pageNumbers = Array.from({ length: Math.min(7, totalPages) }, (_, index) => Math.max(1, Math.min(totalPages, state.page - 3 + index)))
    .filter((value, index, values) => values.indexOf(value) === index)

  return (
    <PageShell title={t('hack.list.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
      {state.errorMessage ? <HackErrorAlert message={state.errorMessage} /> : null}

      <div className="mb-6">
        <PaginationControls
          currentPage={state.page}
          pageNumbers={pageNumbers}
          totalPages={totalPages}
          previousLabel={t('submission.pagination.previous')}
          nextLabel={t('submission.pagination.next')}
          onPageChange={(page) => dispatch({ type: 'page', page })}
        />
      </div>

      <div className="space-y-4">
        {state.isLoading || state.hacks.length === 0 ? (
          <HackCard>
            <CardContent className="py-10 text-sm text-slate-500">{state.isLoading ? t('hack.list.loading') : t('hack.list.empty')}</CardContent>
          </HackCard>
        ) : (
          state.hacks.map((hack) => (
            <HackCard key={hackIdValue(hack.id)}>
              <CardContent className="py-3.5">
                <dl className="grid gap-2 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-[minmax(0,2fr)_minmax(0,4fr)_minmax(0,3fr)_minmax(0,3fr)_minmax(0,2fr)_minmax(0,3fr)_minmax(0,4fr)]">
                  <Field label={t('hack.id')} value={<Link className="font-medium text-slate-900 hover:underline" to={`/hacks/${hackIdValue(hack.id)}`}>#{hackIdValue(hack.id)}</Link>} />
                  <Field label={t('submission.list.problem')} value={<Link className="font-medium text-slate-900 hover:underline" to={`/problems/${problemSlugValue(hack.problemSlug)}`}>{hack.problemTitle}</Link>} />
                  <Field label={t('hack.targetSubmission')} value={<Link className="font-medium text-slate-900 hover:underline" to={`/submissions/${submissionIdValue(hack.targetSubmissionId)}`}>#{submissionIdValue(hack.targetSubmissionId)}</Link>} />
                  <Field label={t('hack.author')} value={<UserProfileLink user={hack.author} />} />
                  <Field label={t('hack.status')} value={hackStatusLabel(hack.status, t)} />
                  <Field label={t('hack.oldScore')} value={formatOptionalScore(hack.oldScore)} />
                  <Field label={t('common.submittedAt')} value={<DateTimeText value={hack.createdAt} />} />
                </dl>
              </CardContent>
            </HackCard>
          ))
        )}
      </div>
    </PageShell>
  )
}

/**
 * Hack 列表字段展示组件，渲染紧凑的标签和值。
 */
function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="mt-1 min-h-[1.625rem] py-1 font-medium text-slate-900">{value}</dd>
    </div>
  )
}
