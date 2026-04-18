import { useEffect, useState } from 'react'

import { getBlog } from '@/features/blog/api/blog-client'
import type { BlogDetail, BlogId } from '@/features/blog/domain/blog'

export function useBlogDetailQuery(blogId: BlogId | null) {
  const [blog, setBlog] = useState<BlogDetail | null>(null)
  const [isLoading, setIsLoading] = useState(Boolean(blogId))
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!blogId) {
      setBlog(null)
      setIsLoading(false)
      setErrorMessage('invalid')
      return
    }

    let cancelled = false
    setBlog(null)
    setIsLoading(true)
    setErrorMessage('')

    void getBlog(blogId)
      .then((loadedBlog) => {
        if (cancelled) {
          return
        }

        setBlog(loadedBlog)
        setIsLoading(false)
      })
      .catch(() => {
        if (cancelled) {
          return
        }

        setBlog(null)
        setIsLoading(false)
        setErrorMessage('loadFailed')
      })

    return () => {
      cancelled = true
    }
  }, [blogId])

  return {
    blog,
    setBlog,
    isLoading,
    errorMessage,
  }
}
