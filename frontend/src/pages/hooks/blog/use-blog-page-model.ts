import { useCallback, useEffect, useState } from 'react'

import { acceptBlogProblemSubmission } from '@/apis/blog/AcceptBlogProblemSubmission'
import { linkBlogToProblem } from '@/apis/blog/LinkBlogToProblem'
import { listPendingProblemBlogs } from '@/apis/blog/ListPendingProblemBlogs'
import { unlinkBlogFromProblem } from '@/apis/blog/UnlinkBlogFromProblem'
import { parseBlogId } from '@/objects/blog/blog-parsers'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import { useBlogListQuery } from '@/pages/hooks/blog/use-blog-list-query'
import type { Username } from '@/objects/user/Username'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useI18n } from '@/system/i18n/use-i18n'
import type { PageRequest } from '@/objects/shared/PageRequest'

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
  const [activeReviewBlogId, setActiveReviewBlogId] = useState<BlogId | null>(null)

  const refreshPendingBlogs = useCallback((isCancelled: () => boolean = () => false) => {
    if (!canManageProblemLinks || !problemSlugFilter) {
      setPendingBlogs([])
      return
    }

    setIsLoadingPending(true)
    setPendingMessage('')
    void listPendingProblemBlogs(problemSlugFilter, { page: 1, pageSize: 10 })
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
      await linkBlogToProblem(problemSlugFilter, parsedBlogId.value)
      model.reload()
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
      await unlinkBlogFromProblem(problemSlugFilter, blogId)
      model.reload()
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
      await acceptBlogProblemSubmission(problemSlugFilter, blog.id)
      model.reload()
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
      await unlinkBlogFromProblem(problemSlugFilter, blog.id)
      setPendingBlogs((blogs) => blogs.filter((item) => item.id !== blog.id))
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
