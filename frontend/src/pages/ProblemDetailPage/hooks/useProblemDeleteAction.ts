import { useCallback, useState } from 'react'

import { DeleteProblem } from '@/apis/problem/DeleteProblem'
import { HttpClientError } from '@/system/api/http-client'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'

export function useProblemDeleteAction(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentProblem = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      await sendAPI(new DeleteProblem(problemSlug, contestSlug))
      return { ok: true, message: 'Problem deleted.' }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to delete problem.'
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [contestSlug, problemSlug])

  return { isDeleting, deleteCurrentProblem }
}
