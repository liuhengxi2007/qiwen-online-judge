import { useCallback, useState } from 'react'

import { DeleteProblemSet } from '@/apis/problemset/DeleteProblemSet'
import { isHttpClientError } from '@/system/api/http-client'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { sendAPI } from '@/system/api/api-message'

export function useProblemSetDeleteAction(problemSetSlug: ProblemSetSlug) {
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentProblemSet = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      await sendAPI(new DeleteProblemSet(problemSetSlug))
      return { ok: true, message: 'Problem set deleted.' }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : 'Unable to delete problem set.'
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [problemSetSlug])

  return { isDeleting, deleteCurrentProblemSet }
}
