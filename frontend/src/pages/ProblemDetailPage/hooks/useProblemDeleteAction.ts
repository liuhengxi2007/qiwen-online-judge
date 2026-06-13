import { useCallback, useState } from 'react'

import { DeleteProblem } from '@/apis/problem/DeleteProblem'
import { isHttpClientError } from '@/system/api/http-client'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'

/**
 * 删除题目动作 hook；按普通或比赛上下文提交删除请求并维护状态。
 */
export function useProblemDeleteAction(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentProblem = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      await sendAPI(new DeleteProblem(problemSlug, contestSlug))
      return { ok: true, message: 'Problem deleted.' }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : 'Unable to delete problem.'
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [contestSlug, problemSlug])

  return { isDeleting, deleteCurrentProblem }
}
