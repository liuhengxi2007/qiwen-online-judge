import { useEffect, useMemo, useState } from 'react'

import type { Username } from '@/objects/user/Username'
import { listBlogs } from '@/apis/blog/ListBlogs'
import { listProblemBlogs } from '@/apis/blog/ListProblemBlogs'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { PageRequest } from '@/objects/shared/PageRequest'

type BlogListQuery = {
  authorUsername: Username | null
  problemSlug: ProblemSlug | null
  pageRequest: PageRequest
}

export function useBlogListQuery(authorUsername: Username | null = null, problemSlug: ProblemSlug | null = null, pageRequest: PageRequest) {
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [queryState, setQueryState] = useState<{
    query: BlogListQuery | null
    reloadToken: number | null
    blogs: BlogSummary[]
    page: number
    pageSize: number
    totalItems: number
    errorMessage: string
  }>({
    query: null,
    reloadToken: null,
    blogs: [],
    page,
    pageSize,
    totalItems: 0,
    errorMessage: '',
  })
  const [reloadToken, setReloadToken] = useState(0)
  const query = useMemo(
    () => ({
      authorUsername,
      problemSlug,
      pageRequest: { page, pageSize },
    }),
    [authorUsername, page, pageSize, problemSlug],
  )
  const hasCurrentQuery = queryState.query === query && queryState.reloadToken === reloadToken

  useEffect(() => {
    let cancelled = false
    const loadBlogs =
      query.problemSlug === null
        ? listBlogs(query.authorUsername, query.pageRequest)
        : listProblemBlogs(query.problemSlug, query.pageRequest)

    void loadBlogs
      .then((response) => {
        if (cancelled) {
          return
        }

        setQueryState({
          query,
          reloadToken,
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
          query,
          reloadToken,
          blogs: [],
          page: query.pageRequest.page,
          pageSize: query.pageRequest.pageSize,
          totalItems: 0,
          errorMessage: 'Unable to load blogs.',
        })
      })

    return () => {
      cancelled = true
    }
  }, [query, reloadToken])

  return {
    blogs: hasCurrentQuery ? queryState.blogs : [],
    page: hasCurrentQuery ? queryState.page : page,
    pageSize: hasCurrentQuery ? queryState.pageSize : pageSize,
    totalItems: hasCurrentQuery ? queryState.totalItems : 0,
    isLoading: !hasCurrentQuery,
    errorMessage: hasCurrentQuery ? queryState.errorMessage : '',
    reload: () => setReloadToken((value) => value + 1),
  }
}
