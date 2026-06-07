import { useEffect, useReducer, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { FileText, Upload } from 'lucide-react'
import { CreateHack, CreateHackMultipart } from '@/apis/hack/CreateHack'
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
import {
  canSubmitHackSources,
  type HackSourceMode,
  usesHackMultipart,
} from './functions/HackSourceMode'
import { sendAPI, sendMultipartAPI } from '@/system/api/api-message'
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
  const [inputMode, setInputMode] = useState<HackSourceMode>('paste')
  const [inputFile, setInputFile] = useState<File | null>(null)
  const [strategyProviderSource, setStrategyProviderSource] = useState('')
  const [strategyProviderMode, setStrategyProviderMode] = useState<HackSourceMode>('paste')
  const [strategyProviderFile, setStrategyProviderFile] = useState<File | null>(null)
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

    const inputSource = { mode: inputMode, text: input, file: inputFile }
    const strategyProvider = { mode: strategyProviderMode, text: strategyProviderSource, file: strategyProviderFile }
    if (!canSubmitHackSources(inputSource, strategyProvider, state.info.requiresStrategyProvider)) {
      return
    }

    setIsSubmitting(true)
    const request = (() => {
      if (usesHackMultipart(inputSource, strategyProvider, state.info.requiresStrategyProvider)) {
        const api = new CreateHackMultipart({
          targetSubmissionId: submissionId,
          subtaskIndex,
          input: inputMode === 'file'
            ? { kind: 'file', value: inputFile! }
            : { kind: 'text', value: input },
          strategyProviderSource: state.info.requiresStrategyProvider
            ? (strategyProviderMode === 'file'
                ? { kind: 'file' as const, value: strategyProviderFile! }
                : { kind: 'text' as const, value: strategyProviderSource })
            : null,
        })
        return sendMultipartAPI(api, api.formData())
      }

      return sendAPI(new CreateHack({
        targetSubmissionId: submissionId,
        subtaskIndex,
        input,
        strategyProviderSource: state.info.requiresStrategyProvider ? strategyProviderSource : null,
      }))
    })()

    void request
      .then((hack) => dispatch({ type: 'hack_loaded', hack }))
      .catch((error: unknown) => dispatch({ type: 'failed', message: error instanceof HttpClientError ? error.message : t('hack.submit.submitFailed') }))
      .finally(() => setIsSubmitting(false))
  }

  const canSubmit = state.info
    ? canSubmitHackSources(
        { mode: inputMode, text: input, file: inputFile },
        { mode: strategyProviderMode, text: strategyProviderSource, file: strategyProviderFile },
        state.info.requiresStrategyProvider,
      )
    : false

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
                <HackSourceTabs
                  file={inputFile}
                  fileInputId="hack-input-file"
                  mode={inputMode}
                  onFileChange={setInputFile}
                  onModeChange={setInputMode}
                  onTextChange={setInput}
                  text={input}
                  textAreaId="hack-input"
                  disabled={isSubmitting}
                />
              </div>
              {state.info.requiresStrategyProvider ? (
                <div className="space-y-2">
                  <Label htmlFor="hack-strategy">{t('hack.submit.strategyProvider')}</Label>
                  <HackSourceTabs
                    file={strategyProviderFile}
                    fileInputId="hack-strategy-file"
                    mode={strategyProviderMode}
                    onFileChange={setStrategyProviderFile}
                    onModeChange={setStrategyProviderMode}
                    onTextChange={setStrategyProviderSource}
                    text={strategyProviderSource}
                    textAreaId="hack-strategy"
                    disabled={isSubmitting}
                  />
                </div>
              ) : null}
              <Button disabled={isSubmitting || Boolean(state.hack) || !canSubmit} onClick={submit}>
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

type HackSourceTabsProps = {
  disabled: boolean
  file: File | null
  fileInputId: string
  mode: HackSourceMode
  onFileChange: (file: File | null) => void
  onModeChange: (mode: HackSourceMode) => void
  onTextChange: (value: string) => void
  text: string
  textAreaId: string
}

function HackSourceTabs({
  disabled,
  file,
  fileInputId,
  mode,
  onFileChange,
  onModeChange,
  onTextChange,
  text,
  textAreaId,
}: HackSourceTabsProps) {
  const { t } = useI18n()

  return (
    <Tabs
      value={mode}
      onValueChange={(value) => {
        if (value === 'paste' || value === 'file') {
          onModeChange(value)
        }
      }}
    >
      <TabsList className="h-9 rounded-lg">
        <TabsTrigger value="paste" className="gap-2 rounded-md">
          <FileText className="size-4" />
          {t('hack.submit.sourcePaste')}
        </TabsTrigger>
        <TabsTrigger value="file" className="gap-2 rounded-md">
          <Upload className="size-4" />
          {t('hack.submit.sourceFile')}
        </TabsTrigger>
      </TabsList>
      <TabsContent value="paste" className="mt-3">
        <Textarea id={textAreaId} className="min-h-48 font-mono" value={text} onChange={(event) => onTextChange(event.target.value)} />
      </TabsContent>
      <TabsContent value="file" className="mt-3">
        <div className="flex flex-wrap items-center gap-3 rounded-lg border border-dashed border-slate-300 p-4">
          <Input
            id={fileInputId}
            type="file"
            disabled={disabled}
            className="sr-only"
            onChange={(event) => {
              const selectedFile = event.currentTarget.files?.[0] ?? null
              event.currentTarget.value = ''
              onFileChange(selectedFile)
            }}
          />
          <Button
            asChild
            variant="outline"
            className={`h-9 rounded-lg border-slate-300 bg-white px-3 ${disabled ? 'pointer-events-none opacity-50' : ''}`}
          >
            <label htmlFor={fileInputId} aria-disabled={disabled}>
              <Upload className="size-4" />
              {t('hack.submit.chooseFile')}
            </label>
          </Button>
          <span className="min-w-0 text-sm text-slate-600">{file ? file.name : t('hack.submit.noFileSelected')}</span>
          {file ? (
            <Button type="button" variant="ghost" disabled={disabled} className="h-9 rounded-lg px-3" onClick={() => onFileChange(null)}>
              {t('hack.submit.clearFile')}
            </Button>
          ) : null}
        </div>
      </TabsContent>
    </Tabs>
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
