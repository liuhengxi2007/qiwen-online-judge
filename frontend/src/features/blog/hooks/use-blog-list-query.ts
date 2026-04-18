import { useEffect, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import { listBlogs, listProblemBlogs } from '@/features/blog/api/blog-client'
import type { BlogSummary } from '@/features/blog/domain/blog'
import type { ProblemSlug } from '@/features/problem/domain/problem'

export function useBlogListQuery(authorUsername: Username | null = null, problemSlug: ProblemSlug | null = null) {
  const [blogs, setBlogs] = useState<BlogSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    setErrorMessage('')

    const loadBlogs = problemSlug === null ? listBlogs(authorUsername) : listProblemBlogs(problemSlug)

    void loadBlogs
      .then((loadedBlogs) => {
        if (cancelled) {
          return
        }

        setBlogs(loadedBlogs)
        setIsLoading(false)
      })
      .catch(() => {
        if (cancelled) {
          return
        }

        setBlogs([])
        setIsLoading(false)
        setErrorMessage('Unable to load blogs.')
      })

    return () => {
      cancelled = true
    }
  }, [authorUsername, problemSlug])

  return {
    blogs,
    isLoading,
    errorMessage,
  }
}
