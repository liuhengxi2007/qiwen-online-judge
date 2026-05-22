import { useEffect, useState } from 'react'

import type { Username } from '@/features/user/domain/user'
import { listBlogs, listProblemBlogs } from '@/features/blog/http/api/blog-client'
import type { BlogSummary } from '@/features/blog/domain/blog'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import type { PageRequest } from '@/shared/model/Pagination'

export function useBlogListQuery(authorUsername: Username | null = null, problemSlug: ProblemSlug | null = null, pageRequest: PageRequest) {
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
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
    page,
    pageSize,
    totalItems: 0,
    errorMessage: '',
  })
  const [reloadToken, setReloadToken] = useState(0)
  const queryKey = `${authorUsername ?? ''}:${problemSlug ?? ''}:${page}:${pageSize}:${reloadToken}`

  useEffect(() => {
    let cancelled = false
    const nextPageRequest = { page, pageSize }
    const loadBlogs = problemSlug === null ? listBlogs(authorUsername, nextPageRequest) : listProblemBlogs(problemSlug, nextPageRequest)

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
          page,
          pageSize,
          totalItems: 0,
          errorMessage: 'Unable to load blogs.',
        })
      })

    return () => {
      cancelled = true
    }
  }, [authorUsername, page, pageSize, problemSlug, queryKey])

  return {
    blogs: queryState.key === queryKey ? queryState.blogs : [],
    page: queryState.key === queryKey ? queryState.page : page,
    pageSize: queryState.key === queryKey ? queryState.pageSize : pageSize,
    totalItems: queryState.key === queryKey ? queryState.totalItems : 0,
    isLoading: queryState.key !== queryKey,
    errorMessage: queryState.key === queryKey ? queryState.errorMessage : '',
    reload: () => setReloadToken((value) => value + 1),
  }
}
