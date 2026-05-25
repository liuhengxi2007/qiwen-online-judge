import { useEffect, useState } from 'react'

import { getSubmission } from '@/features/submission/http/api/GetSubmission'
import { listSubmissions } from '@/features/submission/http/api/ListSubmissions'
import { isTerminalSubmissionStatus } from '@/features/submission/lib/submission-parsers'
import type { SubmissionListRequest } from '@/features/submission/http/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/features/submission/http/response/SubmissionListResponse'
import type { SubmissionSummary } from '@/features/submission/http/response/SubmissionSummary'

function requestKey(request: SubmissionListRequest): string {
  return JSON.stringify({
    userQuery: request.userQuery,
    problemQuery: request.problemQuery,
    verdict: request.verdict,
    sort: request.sort,
    direction: request.direction,
    pageRequest: {
      page: request.pageRequest.page,
      pageSize: request.pageRequest.pageSize,
    },
  })
}

export function useSubmissionListQuery(request: SubmissionListRequest) {
  const key = requestKey(request)
  const fallbackPage = request.pageRequest.page
  const fallbackPageSize = request.pageRequest.pageSize
  const [queryState, setQueryState] = useState<{
    key: string
    response: SubmissionListResponse
    errorMessage: string
  }>({
    key: '',
    response: {
      items: [],
      page: fallbackPage,
      pageSize: fallbackPageSize,
      totalItems: 0,
    },
    errorMessage: '',
  })

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null
    let refreshInFlight = false
    const activeRequest = JSON.parse(key) as SubmissionListRequest
    let currentResponse: SubmissionListResponse = {
      items: [],
      page: activeRequest.pageRequest.page,
      pageSize: activeRequest.pageRequest.pageSize,
      totalItems: 0,
    }

    function mergeSubmissionSummary(summary: SubmissionSummary, detail: Awaited<ReturnType<typeof getSubmission>>): SubmissionSummary {
      return {
        ...summary,
        status: detail.status,
        verdict: detail.verdict,
        timeUsedMs: detail.timeUsedMs,
        memoryUsedKb: detail.memoryUsedKb,
        score: detail.score,
        startedAt: detail.startedAt,
        finishedAt: detail.finishedAt,
      }
    }

    function stopPollingWhenStable(response: SubmissionListResponse) {
      if (!response.items.some((submission) => !isTerminalSubmissionStatus(submission.status)) && intervalId !== null) {
        window.clearInterval(intervalId)
        intervalId = null
      }
    }

    const refreshVisibleSubmissionStates = () => {
      if (refreshInFlight) {
        return
      }

      const activeSubmissions = currentResponse.items.filter((submission) => !isTerminalSubmissionStatus(submission.status))
      if (activeSubmissions.length === 0) {
        stopPollingWhenStable(currentResponse)
        return
      }

      refreshInFlight = true
      void Promise.allSettled(activeSubmissions.map((submission) => getSubmission(submission.id))).then((results) => {
        refreshInFlight = false
        if (cancelled) {
          return
        }

        const refreshedById = new Map(
          results.flatMap((result, index) =>
            result.status === 'fulfilled' ? [[activeSubmissions[index].id, result.value] as const] : [],
          ),
        )

        if (refreshedById.size === 0) {
          return
        }

        currentResponse = {
          ...currentResponse,
          items: currentResponse.items.map((submission) => {
            const refreshed = refreshedById.get(submission.id)
            return refreshed ? mergeSubmissionSummary(submission, refreshed) : submission
          }),
        }

        setQueryState((previousState) => ({
          key,
          response: currentResponse,
          errorMessage: previousState.key === key ? previousState.errorMessage : '',
        }))
        stopPollingWhenStable(currentResponse)
      })
    }

    const load = () => {
      void listSubmissions(activeRequest)
        .then((loadedResponse) => {
          if (cancelled) {
            return
          }

          currentResponse = loadedResponse
          setQueryState({
            key,
            response: currentResponse,
            errorMessage: '',
          })
          stopPollingWhenStable(currentResponse)
        })
        .catch(() => {
          if (cancelled) {
            return
          }

          setQueryState({
            key,
            response: {
              items: [],
              page: activeRequest.pageRequest.page,
              pageSize: activeRequest.pageRequest.pageSize,
              totalItems: 0,
            },
            errorMessage: 'Unable to load submissions.',
          })
        })
    }

    load()
    intervalId = window.setInterval(refreshVisibleSubmissionStates, 3000)

    return () => {
      cancelled = true
      if (intervalId !== null) {
        window.clearInterval(intervalId)
      }
    }
  }, [key])

  return {
    response:
      queryState.key === key
        ? queryState.response
        : {
            items: [],
            page: fallbackPage,
            pageSize: fallbackPageSize,
            totalItems: 0,
          },
    isLoading: queryState.key !== key,
    errorMessage: queryState.key === key ? queryState.errorMessage : '',
  }
}
