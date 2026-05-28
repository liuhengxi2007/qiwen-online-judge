import { useEffect, useState } from 'react'

import { GetBlog } from '@/apis/blog/GetBlog'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { sendAPI } from '@/system/api/api-message'

export function useBlogDetailQuery(blogId: BlogId | null) {
  const [queryState, setQueryState] = useState<{
    blogId: BlogId | null
    blog: BlogDetail | null
    errorMessage: string
  }>({
    blogId: null,
    blog: null,
    errorMessage: '',
  })

  useEffect(() => {
    if (!blogId) {
      return
    }

    let cancelled = false

    void sendAPI(new GetBlog(blogId))
      .then((loadedBlog) => {
        if (cancelled) {
          return
        }

        setQueryState({
          blogId,
          blog: loadedBlog,
          errorMessage: '',
        })
      })
      .catch(() => {
        if (cancelled) {
          return
        }

        setQueryState({
          blogId,
          blog: null,
          errorMessage: 'loadFailed',
        })
      })

    return () => {
      cancelled = true
    }
  }, [blogId])

  return {
    blog: queryState.blogId === blogId ? queryState.blog : null,
    setBlog: (blog: BlogDetail | null) =>
      setQueryState((currentState) => ({
        ...currentState,
        blogId,
        blog,
      })),
    isLoading: blogId !== null && queryState.blogId !== blogId,
    errorMessage: blogId === null ? 'invalid' : queryState.blogId === blogId ? queryState.errorMessage : '',
  }
}
