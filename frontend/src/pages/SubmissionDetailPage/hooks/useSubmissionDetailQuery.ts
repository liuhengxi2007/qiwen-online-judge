import { useEffect, useReducer } from 'react'

import { GetSubmissionHackAvailability } from '@/apis/hack/GetSubmissionHackAvailability'
import { GetSubmission } from '@/apis/submission/GetSubmission'
import { isTerminalSubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionHackAvailability } from '@/objects/hack/response/SubmissionHackAvailability'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'

/**
 * 提交详情查询状态，包含提交详情、hack 可用性、加载标记和页面错误。
 */
type SubmissionDetailQueryState = {
  submission: SubmissionDetail | null
  hackAvailability: SubmissionHackAvailability | null
  isLoading: boolean
  errorMessage: string
}

/**
 * 提交详情查询 reducer 动作，用于加载、失败和外部操作后替换提交详情。
 */
type SubmissionDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; submission: SubmissionDetail; hackAvailability: SubmissionHackAvailability | null }
  | { type: 'load_failed'; message: string }
  | { type: 'replace_submission'; submission: SubmissionDetail }

const initialState: SubmissionDetailQueryState = {
  submission: null,
  hackAvailability: null,
  isLoading: true,
  errorMessage: '',
}

/**
 * 提交详情查询 reducer，保持提交详情和 hack 可用性在同一状态快照中更新。
 */
function reducer(state: SubmissionDetailQueryState, action: SubmissionDetailQueryAction): SubmissionDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { submission: action.submission, hackAvailability: action.hackAvailability, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { submission: null, hackAvailability: null, isLoading: false, errorMessage: action.message }
    case 'replace_submission':
      return { submission: action.submission, hackAvailability: null, isLoading: false, errorMessage: '' }
  }
}

/**
 * 提交详情查询 hook，按提交 id 加载详情并在非终态时每 2 秒轮询。
 * 当提交完成且有裁判结果时额外查询 hack 可用性；组件卸载或 id 变化后丢弃过期响应。
 */
export function useSubmissionDetailQuery(submissionId: SubmissionId) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null
    dispatch({ type: 'load_started' })

    const load = () => {
      void sendAPI(new GetSubmission(submissionId))
        .then(async (submission) => {
          if (cancelled) {
            return
          }

          const hackAvailability =
            submission.status === 'completed' && submission.judgeResult
              ? await sendAPI(new GetSubmissionHackAvailability(submission.id)).catch(() => null)
              : null

          if (cancelled) {
            return
          }

          dispatch({ type: 'load_succeeded', submission, hackAvailability })
          if (isTerminalSubmissionStatus(submission.status) && intervalId !== null) {
            window.clearInterval(intervalId)
            intervalId = null
          }
        })
        .catch((error: unknown) => {
          if (cancelled) {
            return
          }

          dispatch({
            type: 'load_failed',
            message:
              // 注意：提交详情把 forbidden 和 not-found 都展示为 404，用于隐藏不可见提交是否存在。
              isHttpClientError(error) && (error.kind === 'not-found' || error.kind === 'forbidden')
                ? '404 Not Found.'
                : 'Unable to load submission details.',
          })
        })
    }

    load()
    intervalId = window.setInterval(load, 2000)

    return () => {
      cancelled = true
      if (intervalId !== null) {
        window.clearInterval(intervalId)
      }
    }
  }, [submissionId])

  return {
    ...state,
    replaceSubmission: (submission: SubmissionDetail) => dispatch({ type: 'replace_submission', submission }),
  }
}
