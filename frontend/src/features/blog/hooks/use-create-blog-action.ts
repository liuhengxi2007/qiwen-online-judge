import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { createBlog } from '@/features/blog/http/api/CreateBlog'
import type { CreateBlogRequest } from '@/features/blog/http/request/CreateBlogRequest'
import { blogIdValue } from '@/features/blog/lib/blog-parsers'

export function useCreateBlogAction(createFailedMessage: string) {
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const submit = useCallback(
    async (request: CreateBlogRequest) => {
      setIsSubmitting(true)
      setErrorMessage('')
      try {
        const createdBlog = await createBlog(request)
        navigate(`/blogs/${blogIdValue(createdBlog.id)}`)
      } catch {
        setErrorMessage(createFailedMessage)
      } finally {
        setIsSubmitting(false)
      }
    },
    [createFailedMessage, navigate],
  )

  return {
    isSubmitting,
    errorMessage,
    setErrorMessage,
    submit,
  }
}
