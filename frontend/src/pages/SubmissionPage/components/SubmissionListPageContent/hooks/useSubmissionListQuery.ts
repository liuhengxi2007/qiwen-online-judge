import { useEffect, useState } from 'react'

import { GetSubmission } from '@/apis/submission/GetSubmission'
import { ListContestSubmissions } from '@/apis/submission/ListContestSubmissions'
import { ListSubmissions } from '@/apis/submission/ListSubmissions'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { isTerminalSubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/objects/submission/response/SubmissionListResponse'
import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import { sendAPI } from '@/system/api/api-message'

/**
 * 为提交列表请求生成稳定 key，确保 effect 只在筛选、排序、分页或比赛范围变化时重跑。
 */
function requestKey(request: SubmissionListRequest, contestSlug?: ContestSlug): string {
  return JSON.stringify({
    contestSlug: contestSlug ? contestSlugValue(contestSlug) : null,
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

/**
 * 加载提交列表并轮询刷新非终态提交；返回当前请求对应的响应、加载状态和错误文案。
 */
export function useSubmissionListQuery(request: SubmissionListRequest, contestSlug?: ContestSlug) {
  const key = requestKey(request, contestSlug)
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
    const activeRequest = request
    const activeContestSlug = contestSlug
    let currentResponse: SubmissionListResponse = {
      items: [],
      page: activeRequest.pageRequest.page,
      pageSize: activeRequest.pageRequest.pageSize,
      totalItems: 0,
    }

    function mergeSubmissionSummary(summary: SubmissionSummary, detail: SubmissionDetail): SubmissionSummary {
      return {
        ...summary,
        status: detail.status,
        verdict: detail.verdict,
        resultDisplayMode: detail.resultDisplayMode,
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
      void Promise.allSettled(activeSubmissions.map((submission) => sendAPI(new GetSubmission(submission.id)))).then((results) => {
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
      void sendAPI(activeContestSlug ? new ListContestSubmissions(activeContestSlug, activeRequest) : new ListSubmissions(activeRequest))
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
  }, [contestSlug, key, request])

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
