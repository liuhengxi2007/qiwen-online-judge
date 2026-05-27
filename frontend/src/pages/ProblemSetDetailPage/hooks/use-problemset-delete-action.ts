import { useCallback, useState } from 'react'

import { HttpClientError } from '@/system/api/http-client'
import { deleteProblemSet } from '@/apis/problemset/DeleteProblemSet'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'

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
