import { useCallback, useState } from 'react'

import { HttpClientError } from '@/system/api/http-client'
import { deleteProblem } from '@/apis/problem/DeleteProblem'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'

export function useProblemDeleteAction(problemSlug: ProblemSlug) {
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentProblem = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      await deleteProblem(problemSlug)
      return { ok: true, message: 'Problem deleted.' }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to delete problem.'
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [problemSlug])

  return { isDeleting, deleteCurrentProblem }
}
