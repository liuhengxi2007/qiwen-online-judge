import { useEffect, useState } from 'react'

import {
  acceptBlogProblemSubmission,
  linkBlogToProblem,
  listPendingProblemBlogs,
  unlinkBlogFromProblem,
} from '@/features/blog/http/api/blog-client'
import { blogIdValue, parseBlogId, type BlogId, type BlogSummary } from '@/features/blog/domain/blog'
import { useBlogListQuery } from '@/features/blog/hooks/use-blog-list-query'
import type { Username } from '@/features/user/domain/user'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import { useI18n } from '@/shared/i18n/use-i18n'
import type { PageRequest } from '@/shared/model/PageRequest'

type UseBlogPageModelArgs = {
  authorUsernameFilter?: Username
  problemSlugFilter?: ProblemSlug
  canManageProblemLinks: boolean
  pageRequest: PageRequest
}

export function useBlogPageModel({
  authorUsernameFilter,
  problemSlugFilter,
  canManageProblemLinks,
  pageRequest,
}: UseBlogPageModelArgs) {
  const { t } = useI18n()
  const model = useBlogListQuery(authorUsernameFilter ?? null, problemSlugFilter ?? null, pageRequest)
  const [linkBlogId, setLinkBlogId] = useState('')
  const [linkMessage, setLinkMessage] = useState('')
  const [isLinking, setIsLinking] = useState(false)
  const [pendingBlogs, setPendingBlogs] = useState<BlogSummary[]>([])
  const [isLoadingPending, setIsLoadingPending] = useState(false)
  const [pendingMessage, setPendingMessage] = useState('')
  const [activeReviewBlogId, setActiveReviewBlogId] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false
    if (!canManageProblemLinks || !problemSlugFilter) {
      setPendingBlogs([])
      return () => {
        cancelled = true
      }
    }

    setIsLoadingPending(true)
    setPendingMessage('')
    void listPendingProblemBlogs(problemSlugFilter, { page: 1, pageSize: 10 })
      .then((response) => {
        if (!cancelled) {
          setPendingBlogs(response.items)
          setIsLoadingPending(false)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPendingBlogs([])
          setIsLoadingPending(false)
          setPendingMessage(t('blog.problem.pendingLoadFailed'))
        }
      })

    return () => {
      cancelled = true
    }
  }, [canManageProblemLinks, problemSlugFilter, model.blogs, t])

  async function submitLinkBlog() {
    if (!problemSlugFilter) {
      return
    }

    const parsedBlogId = parseBlogId(Number(linkBlogId))
    if (!parsedBlogId.ok) {
      setLinkMessage(parsedBlogId.error)
      return
    }

    setIsLinking(true)
    setLinkMessage('')
    try {
      await linkBlogToProblem(problemSlugFilter, parsedBlogId.value)
      model.reload()
      setLinkBlogId('')
      setLinkMessage(t('blog.problem.linkCreated'))
    } catch {
      setLinkMessage(t('blog.problem.linkFailed'))
    } finally {
      setIsLinking(false)
    }
  }

  async function removeProblemLink(blogId: BlogId) {
    if (!problemSlugFilter || !window.confirm(t('blog.problem.unlinkConfirm'))) {
      return
    }

    try {
      await unlinkBlogFromProblem(problemSlugFilter, blogId)
      model.reload()
    } catch {
      setLinkMessage(t('blog.problem.unlinkFailed'))
    }
  }

  async function acceptPendingBlog(blog: BlogSummary) {
    if (!problemSlugFilter) {
      return
    }

    setActiveReviewBlogId(blogIdValue(blog.id))
    setPendingMessage('')
    try {
      await acceptBlogProblemSubmission(problemSlugFilter, blog.id)
      model.reload()
      setPendingBlogs((blogs) => blogs.filter((item) => blogIdValue(item.id) !== blogIdValue(blog.id)))
      setPendingMessage(t('blog.problem.accepted'))
    } catch {
      setPendingMessage(t('blog.problem.acceptFailed'))
    } finally {
      setActiveReviewBlogId(null)
    }
  }

  async function rejectPendingBlog(blog: BlogSummary) {
    if (!problemSlugFilter || !window.confirm(t('blog.problem.rejectConfirm'))) {
      return
    }

    setActiveReviewBlogId(blogIdValue(blog.id))
    setPendingMessage('')
    try {
      await unlinkBlogFromProblem(problemSlugFilter, blog.id)
      setPendingBlogs((blogs) => blogs.filter((item) => blogIdValue(item.id) !== blogIdValue(blog.id)))
      setPendingMessage(t('blog.problem.rejected'))
    } catch {
      setPendingMessage(t('blog.problem.rejectFailed'))
    } finally {
      setActiveReviewBlogId(null)
    }
  }

  return {
    ...model,
    linkBlogId,
    setLinkBlogId,
    linkMessage,
    isLinking,
    pendingBlogs,
    isLoadingPending,
    pendingMessage,
    activeReviewBlogId,
    submitLinkBlog,
    removeProblemLink,
    acceptPendingBlog,
    rejectPendingBlog,
  }
}
