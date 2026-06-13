import { useEffect, useReducer, useState } from 'react'

import { CreateHack, CreateHackMultipart } from '@/apis/hack/CreateHack'
import { GetHack } from '@/apis/hack/GetHack'
import { GetSubmissionHackSubtask } from '@/apis/hack/GetSubmissionHackSubtask'
import type { HackDetail } from '@/objects/hack/response/HackDetail'
import type { HackSubtaskInfo } from '@/objects/hack/response/HackSubtaskInfo'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { sendAPI, sendMultipartAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'
import {
  canSubmitHackSources,
  type HackSourceMode,
  usesHackMultipart,
} from '../functions/HackSourceMode'

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

type UseSubmissionHackModelArgs = {
  submissionId: SubmissionId
  subtaskIndex: number
}

export function useSubmissionHackModel({ submissionId, subtaskIndex }: UseSubmissionHackModelArgs) {
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
        if (!cancelled) dispatch({ type: 'failed', message: isHttpClientError(error) ? error.message : t('hack.submit.loadFailed') })
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
      .catch((error: unknown) => dispatch({ type: 'failed', message: isHttpClientError(error) ? error.message : t('hack.submit.submitFailed') }))
      .finally(() => setIsSubmitting(false))
  }

  const canSubmit = state.info
    ? canSubmitHackSources(
        { mode: inputMode, text: input, file: inputFile },
        { mode: strategyProviderMode, text: strategyProviderSource, file: strategyProviderFile },
        state.info.requiresStrategyProvider,
      )
    : false

  return {
    info: state.info,
    hack: state.hack,
    isLoading: state.isLoading,
    errorMessage: state.errorMessage,
    input,
    setInput,
    inputMode,
    setInputMode,
    inputFile,
    setInputFile,
    strategyProviderSource,
    setStrategyProviderSource,
    strategyProviderMode,
    setStrategyProviderMode,
    strategyProviderFile,
    setStrategyProviderFile,
    isSubmitting,
    submit,
    canSubmit,
  }
}
