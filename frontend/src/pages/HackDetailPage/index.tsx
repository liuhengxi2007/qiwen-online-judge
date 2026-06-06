import type { ReactNode } from 'react'
import { useEffect, useReducer } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { GetHack } from '@/apis/hack/GetHack'
import type { HackDetail } from '@/objects/hack/response/HackDetail'
import type { HackId } from '@/objects/hack/HackId'
import { hackIdValue, parseHackId } from '@/objects/hack/HackId'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { formatOptionalScore } from '@/pages/objects/SubmissionDisplay'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type State = {
  hack: HackDetail | null
  isLoading: boolean
  errorMessage: string
}

type Action = { type: 'loaded'; hack: HackDetail } | { type: 'failed'; message: string }

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'loaded':
      return { hack: action.hack, isLoading: false, errorMessage: '' }
    case 'failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

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
            dispatch({ type: 'failed', message: error instanceof HttpClientError ? error.message : t('hack.detail.loadFailed') })
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
      {state.errorMessage ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{state.errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      {state.isLoading ? (
        <PageLoadingCard message={t('hack.detail.loading')} />
      ) : state.hack ? (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">#{hackIdValue(state.hack.id)}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
              <Metric label={t('submission.list.problem')} value={<Link className="hover:underline" to={`/problems/${problemSlugValue(state.hack.problemSlug)}`}>{state.hack.problemTitle}</Link>} />
              <Metric label={t('hack.targetSubmission')} value={<Link className="hover:underline" to={`/submissions/${submissionIdValue(state.hack.targetSubmissionId)}`}>#{submissionIdValue(state.hack.targetSubmissionId)}</Link>} />
              <Metric label={t('hack.author')} value={<UserProfileLink user={state.hack.author} />} />
              <Metric label={t('hack.status')} value={state.hack.status} />
              <Metric label={t('hack.oldScore')} value={formatOptionalScore(state.hack.oldScore)} />
              <Metric label={t('hack.newScore')} value={formatOptionalScore(state.hack.newScore)} />
              <Metric label={t('hack.validator')} value={state.hack.validatorMessage ?? '--'} />
              <Metric label={t('hack.standard')} value={state.hack.standardMessage ?? '--'} />
              <Metric label={t('hack.targetRun')} value={state.hack.targetMessage ?? '--'} />
              <Metric label={t('common.submittedAt')} value={<DateTimeText value={state.hack.createdAt} />} />
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">{t('hack.submit.input')}</CardTitle>
            </CardHeader>
            <CardContent>
              <pre className="max-h-[28rem] overflow-auto rounded-md bg-slate-950 p-4 text-sm text-slate-50">{state.hack.input}</pre>
            </CardContent>
          </Card>
        </div>
      ) : null}
    </PageShell>
  )
}

function Metric({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-md border border-slate-200 px-3 py-2">
      <p className="text-xs uppercase tracking-normal text-slate-500">{label}</p>
      <div className="mt-1 font-medium text-slate-950">{value}</div>
    </div>
  )
}
