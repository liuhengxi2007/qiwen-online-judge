import { useEffect, useReducer } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'

import { CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { GetHack } from '@/apis/hack/GetHack'
import type { HackDetail } from '@/objects/hack/response/HackDetail'
import type { HackId } from '@/objects/hack/HackId'
import { hackIdValue, parseHackId } from '@/objects/hack/HackId'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { HackCard, HackErrorAlert } from '@/pages/components/HackCard'
import { HackMetric } from '@/pages/components/HackMetric'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { hackStatusLabel } from '@/pages/objects/HackDisplay'
import { formatOptionalScore } from '@/pages/objects/SubmissionDisplay'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * Hack 详情页查询状态，保存详情、加载标记和错误消息。
 */
type State = {
  hack: HackDetail | null
  isLoading: boolean
  errorMessage: string
}

/**
 * Hack 详情页 reducer 动作，覆盖加载成功和失败。
 */
type Action = { type: 'loaded'; hack: HackDetail } | { type: 'failed'; message: string }

/**
 * Hack 详情页 reducer；纯函数维护详情加载状态。
 */
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'loaded':
      return { hack: action.hack, isLoading: false, errorMessage: '' }
    case 'failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

/**
 * Hack 详情页入口，校验 hackId 路由参数后渲染详情内容。
 */
export function HackDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('hack.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { hackId } = useParams<{ hackId: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const hackIdResult = parseHackId(Number(hackId ?? ''))
  if (!hackIdResult.ok) {
    return <Navigate replace to="/hacks" />
  }

  return <HackDetailPageContent hackId={hackIdResult.value} />
}

/**
 * Hack 详情页主体，加载 Hack 明细并展示参与者、目标提交和评测消息。
 */
function HackDetailPageContent({ hackId }: { hackId: HackId }) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, { hack: null, isLoading: true, errorMessage: '' })

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null
    const load = () => {
      void sendAPI(new GetHack(hackId))
        .then((hack) => {
          if (cancelled) return
          dispatch({ type: 'loaded', hack })
          if (hack.finishedAt !== null && intervalId !== null) {
            window.clearInterval(intervalId)
            intervalId = null
          }
        })
        .catch((error: unknown) => {
          if (!cancelled) {
            dispatch({ type: 'failed', message: isHttpClientError(error) ? error.message : t('hack.detail.loadFailed') })
          }
        })
    }
    load()
    intervalId = window.setInterval(load, 2000)
    return () => {
      cancelled = true
      if (intervalId !== null) window.clearInterval(intervalId)
    }
  }, [hackId, t])

  return (
    <PageShell title={t('hack.detail.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
      {state.errorMessage ? <HackErrorAlert message={state.errorMessage} /> : null}

      {state.isLoading ? (
        <PageLoadingCard message={t('hack.detail.loading')} />
      ) : state.hack ? (
        <div className="space-y-6">
          <HackCard>
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">#{hackIdValue(state.hack.id)}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
              <HackMetric label={t('submission.list.problem')} value={<Link className="hover:underline" to={`/problems/${problemSlugValue(state.hack.problemSlug)}`}>{state.hack.problemTitle}</Link>} />
              <HackMetric label={t('hack.targetSubmission')} value={<Link className="hover:underline" to={`/submissions/${submissionIdValue(state.hack.targetSubmissionId)}`}>#{submissionIdValue(state.hack.targetSubmissionId)}</Link>} />
              <HackMetric label={t('hack.author')} value={<UserProfileLink user={state.hack.author} />} />
              <HackMetric label={t('hack.status')} value={hackStatusLabel(state.hack.status, t)} />
              <HackMetric label={t('hack.oldScore')} value={formatOptionalScore(state.hack.oldScore)} />
              <HackMetric label={t('hack.newScore')} value={formatOptionalScore(state.hack.newScore)} />
              <HackMetric label={t('hack.validator')} value={state.hack.validatorMessage ?? '--'} />
              <HackMetric label={t('hack.standard')} value={state.hack.standardMessage ?? '--'} />
              <HackMetric label={t('hack.targetRun')} value={state.hack.targetMessage ?? '--'} />
              <HackMetric label={t('common.submittedAt')} value={<DateTimeText value={state.hack.createdAt} />} />
            </CardContent>
          </HackCard>

          <HackCard>
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">{t('hack.submit.input')}</CardTitle>
            </CardHeader>
            <CardContent>
              <pre className="max-h-[28rem] overflow-auto rounded-md bg-slate-950 p-4 text-sm text-slate-50">{state.hack.input}</pre>
            </CardContent>
          </HackCard>
        </div>
      ) : null}
    </PageShell>
  )
}
