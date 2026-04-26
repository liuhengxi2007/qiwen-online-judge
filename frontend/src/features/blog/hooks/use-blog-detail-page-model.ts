import { useState } from 'react'

import {
  createBlogComment,
  deleteBlog,
  deleteBlogComment,
  submitBlogToProblem,
  updateBlog,
  updateBlogComment,
  voteBlog,
  voteBlogComment,
} from '@/features/blog/api/blog-client'
import {
  blogCommentContentValue,
  parseBlogCommentContent,
  parseBlogContent,
  parseBlogTitle,
  type BlogCommentId,
  type BlogCommentSummary,
  type BlogDetail,
  type BlogVisibility,
  type BlogVote,
} from '@/features/blog/domain/blog'
import { parseProblemSlug } from '@/features/problem/domain/problem'
import { useI18n } from '@/shared/i18n/i18n'

type UseBlogDetailPageModelArgs = {
  blog: BlogDetail | null
  setBlog: (blog: BlogDetail | null) => void
  onDeleted: () => void
}

export function useBlogDetailPageModel({ blog, setBlog, onDeleted }: UseBlogDetailPageModelArgs) {
  const { t } = useI18n()
  const [isVoting, setIsVoting] = useState(false)
  const [votingCommentId, setVotingCommentId] = useState<BlogCommentId | null>(null)
  const [commentContent, setCommentContent] = useState('')
  const [replyTargetId, setReplyTargetId] = useState<BlogCommentId | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [isSubmittingReply, setIsSubmittingReply] = useState(false)
  const [commentErrorMessage, setCommentErrorMessage] = useState('')
  const [isEditingBlog, setIsEditingBlog] = useState(false)
  const [editBlogTitle, setEditBlogTitle] = useState('')
  const [editBlogContent, setEditBlogContent] = useState('')
  const [editBlogVisibility, setEditBlogVisibility] = useState<BlogVisibility>('public')
  const [editingCommentId, setEditingCommentId] = useState<BlogCommentId | null>(null)
  const [editingCommentContent, setEditingCommentContent] = useState('')
  const [submitProblemSlug, setSubmitProblemSlug] = useState('')
  const [isSubmittingToProblem, setIsSubmittingToProblem] = useState(false)
  const [submitProblemMessage, setSubmitProblemMessage] = useState('')

  async function submitVote(vote: BlogVote) {
    if (!blog) {
      return
    }

    setIsVoting(true)
    try {
      setBlog(await voteBlog(blog.id, { vote }))
    } finally {
      setIsVoting(false)
    }
  }

  function startEditingBlog() {
    if (!blog) {
      return
    }

    setEditBlogTitle(blog.title)
    setEditBlogContent(blog.content)
    setEditBlogVisibility(blog.visibility)
    setCommentErrorMessage('')
    setIsEditingBlog(true)
  }

  async function submitBlogEdit() {
    if (!blog) {
      return
    }

    const parsedTitle = parseBlogTitle(editBlogTitle)
    const parsedContent = parseBlogContent(editBlogContent)
    if (!parsedTitle.ok) {
      setCommentErrorMessage(parsedTitle.error)
      return
    }
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setBlog(
      await updateBlog(blog.id, {
        title: parsedTitle.value,
        content: parsedContent.value,
        visibility: editBlogVisibility,
      }),
    )
    setIsEditingBlog(false)
  }

  async function removeBlog() {
    if (!blog || !window.confirm(t('blog.delete.confirm'))) {
      return
    }

    await deleteBlog(blog.id)
    onDeleted()
  }

  async function submitToProblem() {
    if (!blog) {
      return
    }

    const parsedProblemSlug = parseProblemSlug(submitProblemSlug)
    if (!parsedProblemSlug.ok) {
      setSubmitProblemMessage(parsedProblemSlug.error)
      return
    }

    setIsSubmittingToProblem(true)
    setSubmitProblemMessage('')
    try {
      await submitBlogToProblem(parsedProblemSlug.value, blog.id)
      setSubmitProblemSlug('')
      setSubmitProblemMessage(t('blog.problem.submitCreated'))
    } catch {
      setSubmitProblemMessage(t('blog.problem.submitFailed'))
    } finally {
      setIsSubmittingToProblem(false)
    }
  }

  async function submitComment() {
    if (!blog) {
      return
    }

    const parsedContent = parseBlogCommentContent(commentContent)
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setIsSubmittingComment(true)
    setCommentErrorMessage('')
    try {
      setBlog(await createBlogComment(blog.id, { content: parsedContent.value }))
      setCommentContent('')
    } catch {
      setCommentErrorMessage(t('blog.comment.createFailed'))
    } finally {
      setIsSubmittingComment(false)
    }
  }

  async function submitReply(parentCommentId: BlogCommentId) {
    if (!blog) {
      return
    }

    const parsedContent = parseBlogCommentContent(replyContent)
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setIsSubmittingReply(true)
    setCommentErrorMessage('')
    try {
      setBlog(await createBlogComment(blog.id, { content: parsedContent.value }, parentCommentId))
      setReplyTargetId(null)
      setReplyContent('')
    } catch {
      setCommentErrorMessage(t('blog.comment.replyFailed'))
    } finally {
      setIsSubmittingReply(false)
    }
  }

  async function submitCommentVote(commentId: BlogCommentId, vote: BlogVote) {
    if (!blog) {
      return
    }

    setVotingCommentId(commentId)
    try {
      setBlog(await voteBlogComment(blog.id, commentId, { vote }))
    } finally {
      setVotingCommentId(null)
    }
  }

  function startEditingComment(comment: BlogCommentSummary) {
    setEditingCommentId(comment.id)
    setEditingCommentContent(blogCommentContentValue(comment.content))
    setCommentErrorMessage('')
  }

  async function submitCommentEdit(commentId: BlogCommentId) {
    if (!blog) {
      return
    }

    const parsedContent = parseBlogCommentContent(editingCommentContent)
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setBlog(await updateBlogComment(blog.id, commentId, { content: parsedContent.value }))
    setEditingCommentId(null)
    setEditingCommentContent('')
  }

  async function removeComment(commentId: BlogCommentId) {
    if (!blog || !window.confirm(t('blog.comment.deleteConfirm'))) {
      return
    }

    setBlog(await deleteBlogComment(blog.id, commentId))
  }

  return {
    isVoting,
    votingCommentId,
    commentContent,
    setCommentContent,
    replyTargetId,
    setReplyTargetId,
    replyContent,
    setReplyContent,
    isSubmittingComment,
    isSubmittingReply,
    commentErrorMessage,
    setCommentErrorMessage,
    isEditingBlog,
    setIsEditingBlog,
    editBlogTitle,
    setEditBlogTitle,
    editBlogContent,
    setEditBlogContent,
    editBlogVisibility,
    setEditBlogVisibility,
    editingCommentId,
    setEditingCommentId,
    editingCommentContent,
    setEditingCommentContent,
    submitProblemSlug,
    setSubmitProblemSlug,
    isSubmittingToProblem,
    submitProblemMessage,
    submitVote,
    startEditingBlog,
    submitBlogEdit,
    removeBlog,
    submitToProblem,
    submitComment,
    submitReply,
    submitCommentVote,
    startEditingComment,
    submitCommentEdit,
    removeComment,
  }
}
