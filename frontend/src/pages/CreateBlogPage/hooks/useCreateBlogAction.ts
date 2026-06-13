import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { CreateBlog } from '@/apis/blog/CreateBlog'
import type { CreateBlogRequest } from '@/objects/blog/request/CreateBlogRequest'
import { blogIdValue } from '@/objects/blog/BlogId'
import { sendAPI } from '@/system/api/api-message'

/**
 * 创建博客操作 hook，提交创建请求并在成功后导航到新博客详情页。
 * 失败时只设置传入的本地化错误消息，表单草稿由页面保留。
 */
export function useCreateBlogAction(createFailedMessage: string) {
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const submit = useCallback(
    async (request: CreateBlogRequest) => {
      setIsSubmitting(true)
      setErrorMessage('')
      try {
        const createdBlog = await sendAPI(new CreateBlog(request))
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
