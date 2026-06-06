import { useEffect, useReducer, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { CreateHack } from '@/apis/hack/CreateHack'
import { GetHack } from '@/apis/hack/GetHack'
import { GetSubmissionHackSubtask } from '@/apis/hack/GetSubmissionHackSubtask'
import type { HackDetail } from '@/objects/hack/response/HackDetail'
import type { HackSubtaskInfo } from '@/objects/hack/response/HackSubtaskInfo'
import { hackIdValue } from '@/objects/hack/HackId'
import { parseSubmissionId, submissionIdValue } from '@/objects/submission/SubmissionId'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { HackCard, HackErrorAlert } from '@/pages/components/HackCard'
import { HackMetric } from '@/pages/components/HackMetric'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { hackModeLabel, hackStatusLabel } from '@/pages/objects/HackDisplay'
import { formatOptionalScore } from '@/pages/objects/SubmissionDisplay'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type QueryState = {
  info: HackSubtaskInfo | null
  hack: HackDetail | null
  isLoading: boolean
  errorMessage: string
}

type QueryAction =
  | { type: 'info_loaded'; info: HackSubtaskInfo }
  | { type: 'hack_loaded'; hack: HackDetail }
  | { type: 'failed'; message: string }

function reducer(state: QueryState, action: QueryAction): QueryState {
  switch (action.type) {
    case 'info_loaded':
      return { ...state, info: action.info, isLoading: false, errorMessage: '' }
    case 'hack_loaded':
      return { ...state, hack: action.hack, errorMessage: '' }
    case 'failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export function SubmissionHackPage() {
  const { t } = useI18n()
  usePageTitle(t('hack.submit.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { submissionId, subtaskIndex } = useParams<{ submissionId: string; subtaskIndex: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const submissionIdResult = parseSubmissionId(Number(submissionId ?? ''))
  const parsedSubtaskIndex = Number(subtaskIndex ?? '')
  if (!submissionIdResult.ok || !Number.isSafeInteger(parsedSubtaskIndex) || parsedSubtaskIndex < 1) {
    return <Navigate replace to="/submissions" />
  }

  return <SubmissionHackPageContent submissionId={submissionIdResult.value} subtaskIndex={parsedSubtaskIndex} />
}

function SubmissionHackPageContent({ submissionId, subtaskIndex }: { submissionId: SubmissionId; subtaskIndex: number }) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, { info: null, hack: null, isLoading: true, errorMessage: '' })
  const [input, setInput] = useState('')
  const [strategyProviderSource, setStrategyProviderSource] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    void sendAPI(new GetSubmissionHackSubtask(submissionId, subtaskIndex))
      .then((info) => {
        if (!cancelled) dispatch({ type: 'info_loaded', info })
      })
      .catch((error: unknown) => {
        if (!cancelled) dispatch({ type: 'failed', message: error instanceof HttpClientError ? error.message : t('hack.submit.loadFailed') })
      })
    return () => {
      cancelled = true
    }
  }, [submissionId, subtaskIndex, t])

  useEffect(() => {
    if (!state.hack || state.hack.finishedAt !== null) {
      return
    }

    const intervalId = window.setInterval(() => {
      void sendAPI(new GetHack(state.hack!.id)).then((hack) => dispatch({ type: 'hack_loaded', hack })).catch(() => undefined)
    }, 2000)

    return () => window.clearInterval(intervalId)
  }, [state.hack])

  const submit = () => {
    if (!state.info) {
      return
    }

    setIsSubmitting(true)
    void sendAPI(new CreateHack({
      targetSubmissionId: submissionId,
      subtaskIndex,
      input,
      strategyProviderSource: state.info.requiresStrategyProvider ? strategyProviderSource : null,
    }))
      .then((hack) => dispatch({ type: 'hack_loaded', hack }))
      .catch((error: unknown) => dispatch({ type: 'failed', message: error instanceof HttpClientError ? error.message : t('hack.submit.submitFailed') }))
      .finally(() => setIsSubmitting(false))
  }

  return (
    <PageShell title={t('hack.submit.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
      {state.errorMessage ? <HackErrorAlert message={state.errorMessage} /> : null}

      {state.isLoading ? (
        <PageLoadingCard message={t('hack.submit.loading')} />
      ) : state.info ? (
        <div className="space-y-6">
          <HackCard>
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">{t('hack.submit.target')}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
              <HackMetric label={t('submission.list.id')} value={`#${submissionIdValue(state.info.targetSubmissionId)}`} />
              <HackMetric label={t('submission.list.submitter')} value={<UserProfileLink user={state.info.targetSubmitter} />} />
              <HackMetric label={t('hack.subtask')} value={state.info.subtaskLabel ? `${state.info.subtaskIndex} (${state.info.subtaskLabel})` : String(state.info.subtaskIndex)} />
              <HackMetric label={t('hack.subtaskScore')} value={formatOptionalScore(state.info.oldWorstScore)} />
              <HackMetric label={t('hack.mode')} value={hackModeLabel(state.info.mode, t)} />
            </CardContent>
          </HackCard>

          <HackCard>
            <CardHeader>
              <CardTitle className="text-xl text-slate-950">{t('hack.submit.input')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="hack-input">{t('hack.submit.input')}</Label>
                <Textarea id="hack-input" className="min-h-48 font-mono" value={input} onChange={(event) => setInput(event.target.value)} />
              </div>
              {state.info.requiresStrategyProvider ? (
                <div className="space-y-2">
                  <Label htmlFor="hack-strategy">{t('hack.submit.strategyProvider')}</Label>
                  <Textarea id="hack-strategy" className="min-h-48 font-mono" value={strategyProviderSource} onChange={(event) => setStrategyProviderSource(event.target.value)} />
                </div>
              ) : null}
              <Button disabled={isSubmitting || Boolean(state.hack)} onClick={submit}>
                {isSubmitting ? t('hack.submit.submitting') : t('hack.submit.action')}
              </Button>
            </CardContent>
          </HackCard>

          {state.hack ? <HackAttemptPanel hack={state.hack} /> : null}
        </div>
      ) : null}
    </PageShell>
  )
}

function HackAttemptPanel({ hack }: { hack: HackDetail }) {
  const { t } = useI18n()
  return (
    <HackCard>
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">
          <Link className="hover:underline" to={`/hacks/${hackIdValue(hack.id)}`}>#{hackIdValue(hack.id)}</Link>
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
        <HackMetric label={t('hack.status')} value={hackStatusLabel(hack.status, t)} />
        <HackMetric label={t('hack.oldScore')} value={formatOptionalScore(hack.oldScore)} />
        <HackMetric label={t('hack.newScore')} value={formatOptionalScore(hack.newScore)} />
        <HackMetric label={t('hack.validator')} value={hack.validatorMessage ?? '--'} />
        <HackMetric label={t('hack.standard')} value={hack.standardMessage ?? '--'} />
        <HackMetric label={t('hack.targetRun')} value={hack.targetMessage ?? '--'} />
      </CardContent>
    </HackCard>
  )
}
