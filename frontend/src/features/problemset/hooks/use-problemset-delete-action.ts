import { useCallback, useState } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { deleteProblemSet } from '@/features/problemset/http/api/problemset-client'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'

export function useProblemSetDeleteAction(problemSetSlug: ProblemSetSlug) {
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentProblemSet = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      await deleteProblemSet(problemSetSlug)
      return { ok: true, message: 'Problem set deleted.' }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to delete problem set.'
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [problemSetSlug])

  return { isDeleting, deleteCurrentProblemSet }
}
