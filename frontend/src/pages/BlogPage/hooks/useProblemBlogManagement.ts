import { useCallback, useEffect, useState } from 'react'

import { AcceptBlogProblemSubmission } from '@/apis/blog/AcceptBlogProblemSubmission'
import { LinkBlogToProblem } from '@/apis/blog/LinkBlogToProblem'
import { ListPendingProblemBlogs } from '@/apis/blog/ListPendingProblemBlogs'
import { UnlinkBlogFromProblem } from '@/apis/blog/UnlinkBlogFromProblem'
import { parseBlogId } from '@/objects/blog/BlogId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

type UseProblemBlogManagementArgs = {
  canManageProblemLinks: boolean
  problemSlugFilter?: ProblemSlug
  reloadBlogs: () => void
}

export function useProblemBlogManagement({
  canManageProblemLinks,
  problemSlugFilter,
  reloadBlogs,
}: UseProblemBlogManagementArgs) {
  const { t } = useI18n()
  const [linkBlogId, setLinkBlogId] = useState('')
  const [linkMessage, setLinkMessage] = useState('')
  const [isLinking, setIsLinking] = useState(false)
  const [pendingBlogs, setPendingBlogs] = useState<BlogSummary[]>([])
  const [isLoadingPending, setIsLoadingPending] = useState(false)
  const [pendingMessage, setPendingMessage] = useState('')
  const [activeReviewBlogId, setActiveReviewBlogId] = useState<BlogId | null>(null)

  const refreshPendingBlogs = useCallback((isCancelled: () => boolean = () => false) => {
    if (!canManageProblemLinks || !problemSlugFilter) {
      setPendingBlogs([])
      return
    }

    setIsLoadingPending(true)
    setPendingMessage('')
    void sendAPI(new ListPendingProblemBlogs(problemSlugFilter, { page: 1, pageSize: 10 }))
      .then((response) => {
        if (!isCancelled()) {
          setPendingBlogs(response.items)
          setIsLoadingPending(false)
        }
      })
      .catch(() => {
        if (!isCancelled()) {
          setPendingBlogs([])
          setIsLoadingPending(false)
          setPendingMessage(t('blog.problem.pendingLoadFailed'))
        }
      })
  }, [canManageProblemLinks, problemSlugFilter, t])

  useEffect(() => {
    let cancelled = false
    refreshPendingBlogs(() => cancelled)
    return () => {
      cancelled = true
    }
  }, [refreshPendingBlogs])

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
      await sendAPI(new LinkBlogToProblem(problemSlugFilter, parsedBlogId.value))
      reloadBlogs()
      refreshPendingBlogs()
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
      await sendAPI(new UnlinkBlogFromProblem(problemSlugFilter, blogId))
      reloadBlogs()
      refreshPendingBlogs()
    } catch {
      setLinkMessage(t('blog.problem.unlinkFailed'))
    }
  }

  async function acceptPendingBlog(blog: BlogSummary) {
    if (!problemSlugFilter) {
      return
    }

    setActiveReviewBlogId(blog.id)
    setPendingMessage('')
    try {
      await sendAPI(new AcceptBlogProblemSubmission(problemSlugFilter, blog.id))
      reloadBlogs()
      setPendingBlogs((blogs) => blogs.filter((item) => item.id !== blog.id))
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

    setActiveReviewBlogId(blog.id)
    setPendingMessage('')
    try {
      await sendAPI(new UnlinkBlogFromProblem(problemSlugFilter, blog.id))
      setPendingBlogs((blogs) => blogs.filter((item) => item.id !== blog.id))
      setPendingMessage(t('blog.problem.rejected'))
    } catch {
      setPendingMessage(t('blog.problem.rejectFailed'))
    } finally {
      setActiveReviewBlogId(null)
    }
  }

  return {
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
