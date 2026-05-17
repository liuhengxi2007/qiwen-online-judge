import { useEffect, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import { listBlogs, listProblemBlogs } from '@/features/blog/api/blog-client'
import type { BlogSummary } from '@/features/blog/domain/blog'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import type { PageRequest } from '@/shared/model/Pagination'

export function useBlogListQuery(authorUsername: Username | null = null, problemSlug: ProblemSlug | null = null, pageRequest: PageRequest) {
  const [queryState, setQueryState] = useState<{
    key: string | null
    blogs: BlogSummary[]
    page: number
    pageSize: number
    totalItems: number
    errorMessage: string
  }>({
    key: null,
    blogs: [],
    page: pageRequest.page,
    pageSize: pageRequest.pageSize,
    totalItems: 0,
    errorMessage: '',
  })
  const [reloadToken, setReloadToken] = useState(0)
  const queryKey = `${authorUsername ?? ''}:${problemSlug ?? ''}:${pageRequest.page}:${pageRequest.pageSize}:${reloadToken}`

  useEffect(() => {
    let cancelled = false
    const loadBlogs = problemSlug === null ? listBlogs(authorUsername, pageRequest) : listProblemBlogs(problemSlug, pageRequest)

    void loadBlogs
      .then((response) => {
        if (cancelled) {
          return
        }

        setQueryState({
          key: queryKey,
          blogs: response.items,
          page: response.page,
          pageSize: response.pageSize,
          totalItems: response.totalItems,
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
          page: pageRequest.page,
          pageSize: pageRequest.pageSize,
          totalItems: 0,
          errorMessage: 'Unable to load blogs.',
        })
      })

    return () => {
      cancelled = true
    }
  }, [authorUsername, pageRequest.page, pageRequest.pageSize, problemSlug, queryKey])

  return {
    blogs: queryState.key === queryKey ? queryState.blogs : [],
    page: queryState.key === queryKey ? queryState.page : pageRequest.page,
    pageSize: queryState.key === queryKey ? queryState.pageSize : pageRequest.pageSize,
    totalItems: queryState.key === queryKey ? queryState.totalItems : 0,
    isLoading: queryState.key !== queryKey,
    errorMessage: queryState.key === queryKey ? queryState.errorMessage : '',
    reload: () => setReloadToken((value) => value + 1),
  }
}
