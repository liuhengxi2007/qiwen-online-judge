import { useEffect, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import { listBlogs, listProblemBlogs } from '@/features/blog/api/blog-client'
import type { BlogSummary } from '@/features/blog/domain/blog'
import type { ProblemSlug } from '@/features/problem/domain/problem'

export function useBlogListQuery(authorUsername: Username | null = null, problemSlug: ProblemSlug | null = null) {
  const [queryState, setQueryState] = useState<{
    key: string | null
    blogs: BlogSummary[]
    errorMessage: string
  }>({
    key: null,
    blogs: [],
    errorMessage: '',
  })
  const [reloadToken, setReloadToken] = useState(0)
  const queryKey = `${authorUsername ?? ''}:${problemSlug ?? ''}:${reloadToken}`

  useEffect(() => {
    let cancelled = false
    const loadBlogs = problemSlug === null ? listBlogs(authorUsername) : listProblemBlogs(problemSlug)

    void loadBlogs
      .then((loadedBlogs) => {
        if (cancelled) {
          return
        }

        setQueryState({
          key: queryKey,
          blogs: loadedBlogs,
          errorMessage: '',
        })
      })
      .catch(() => {
        if (cancelled) {
          return
        }

        setQueryState({
          key: queryKey,
          blogs: [],
          errorMessage: 'Unable to load blogs.',
        })
      })

    return () => {
      cancelled = true
    }
  }, [authorUsername, problemSlug, queryKey])

  return {
    blogs: queryState.key === queryKey ? queryState.blogs : [],
    isLoading: queryState.key !== queryKey,
    errorMessage: queryState.key === queryKey ? queryState.errorMessage : '',
    reload: () => setReloadToken((value) => value + 1),
  }
}
