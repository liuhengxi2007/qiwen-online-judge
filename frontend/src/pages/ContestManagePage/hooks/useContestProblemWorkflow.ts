import { useCallback, useEffect, useState, type Dispatch } from 'react'

import { AddProblemToContest } from '@/apis/contest/AddProblemToContest'
import { EvaluateContestProblemAttachWarning } from '@/apis/contest/EvaluateContestProblemAttachWarning'
import { RemoveProblemFromContest } from '@/apis/contest/RemoveProblemFromContest'
import { ListManageableProblemSuggestions } from '@/apis/problem/ListManageableProblemSuggestions'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseProblemSlug, problemSlugValue } from '@/objects/problem/ProblemSlug'
import { parseProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'
import type { ContestManagePageAction } from './useContestManagePageModel'

type UseContestProblemWorkflowArgs = {
  attachWarningProblemSlug: ProblemSlug | null
  contestSlug: ContestSlug
  dispatch: Dispatch<ContestManagePageAction>
}

export function useContestProblemWorkflow({ attachWarningProblemSlug, contestSlug, dispatch }: UseContestProblemWorkflowArgs) {
  const { t } = useI18n()
  const [problemSearchInput, setProblemSearchInput] = useState('')
  const [isProblemSearchFocused, setIsProblemSearchFocused] = useState(false)
  const [isLoadingProblemSuggestions, setIsLoadingProblemSuggestions] = useState(false)
  const [problemSuggestions, setProblemSuggestions] = useState<ProblemSuggestion[]>([])

  useEffect(() => {
    if (!isProblemSearchFocused) {
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      const parsedQuery = parseProblemSearchQuery(problemSearchInput)
      if (!parsedQuery.ok) {
        return
      }

      setIsLoadingProblemSuggestions(true)
      void sendAPI(new ListManageableProblemSuggestions(parsedQuery.value, contestSlug))
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
      window.clearTimeout(timeoutId)
    }
  }, [contestSlug, isProblemSearchFocused, problemSearchInput])

  const updateProblemSearchInput = useCallback((value: string) => {
    setProblemSearchInput(value)
    setIsLoadingProblemSuggestions(false)
    setProblemSuggestions([])
  }, [])

  const updateProblemSearchFocus = useCallback((focused: boolean) => {
    setIsProblemSearchFocused(focused)
    if (!focused) {
      setIsLoadingProblemSuggestions(false)
    }
  }, [])

  const attachProblemBySlug = useCallback(async (problemSlug: ProblemSlug) => {
    dispatch({ type: 'attach_problem_started' })
    try {
      const contest = await sendAPI(new AddProblemToContest(contestSlug, { problemSlug }))
      dispatch({ type: 'attach_problem_succeeded', contest, message: t('contest.manage.attachProblemSuccess') })
      setProblemSearchInput('')
      setProblemSuggestions([])
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.manage.attachProblemFailed')
      dispatch({ type: 'attach_problem_failed', message })
    }
  }, [contestSlug, dispatch, t])

  const attachProblemFromInput = useCallback(async (rawInput: string) => {
    const parsedSlug = parseProblemSlug(rawInput)
    if (!parsedSlug.ok) {
      dispatch({ type: 'attach_problem_failed', message: parsedSlug.error })
      return
    }

    dispatch({ type: 'attach_problem_started' })
    try {
      const warning = await sendAPI(new EvaluateContestProblemAttachWarning(contestSlug, parsedSlug.value))
      if (warning.shouldWarn) {
        dispatch({ type: 'attach_problem_warning_opened', problemSlug: parsedSlug.value })
      } else {
        await attachProblemBySlug(parsedSlug.value)
      }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.manage.attachProblemFailed')
      dispatch({ type: 'attach_problem_failed', message })
    }
  }, [attachProblemBySlug, contestSlug, dispatch, t])

  const attachProblem = useCallback(async () => {
    await attachProblemFromInput(problemSearchInput)
  }, [attachProblemFromInput, problemSearchInput])

  const closeAttachProblemWarning = useCallback((open: boolean) => {
    if (!open) {
      dispatch({ type: 'attach_problem_warning_closed' })
    }
  }, [dispatch])

  const confirmAttachProblemWarning = useCallback(async () => {
    if (!attachWarningProblemSlug) {
      return
    }

    dispatch({ type: 'attach_problem_warning_closed' })
    await attachProblemBySlug(attachWarningProblemSlug)
  }, [attachProblemBySlug, attachWarningProblemSlug, dispatch])

  const removeProblem = useCallback(async (rawProblemSlug: string) => {
    const parsedSlug = parseProblemSlug(rawProblemSlug)
    if (!parsedSlug.ok) {
      dispatch({ type: 'remove_problem_failed', message: parsedSlug.error })
      return
    }

    dispatch({ type: 'remove_problem_started', problemSlug: rawProblemSlug })
    try {
      const contest = await sendAPI(new RemoveProblemFromContest(contestSlug, parsedSlug.value))
      dispatch({ type: 'remove_problem_succeeded', contest, message: t('contest.manage.removeProblemSuccess') })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.manage.removeProblemFailed')
      dispatch({ type: 'remove_problem_failed', message })
    }
  }, [contestSlug, dispatch, t])

  const selectProblemSuggestion = useCallback((suggestion: ProblemSuggestion) => {
    setProblemSearchInput(problemSlugValue(suggestion.slug))
    setProblemSuggestions([])
    setIsProblemSearchFocused(false)
  }, [])

  return {
    problemSearchInput,
    isProblemSearchFocused,
    isLoadingProblemSuggestions,
    problemSuggestions: isProblemSearchFocused ? problemSuggestions : [],
    setProblemSearchInput: updateProblemSearchInput,
    setIsProblemSearchFocused: updateProblemSearchFocus,
    selectProblemSuggestion,
    attachProblem,
    closeAttachProblemWarning,
    confirmAttachProblemWarning,
    removeProblem,
  }
}
