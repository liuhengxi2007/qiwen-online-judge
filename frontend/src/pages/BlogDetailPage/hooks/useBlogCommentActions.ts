import { useState } from 'react'

import { CreateBlogComment } from '@/apis/blog/CreateBlogComment'
import { CreateBlogCommentReply } from '@/apis/blog/CreateBlogCommentReply'
import { DeleteBlogComment } from '@/apis/blog/DeleteBlogComment'
import { UpdateBlogComment } from '@/apis/blog/UpdateBlogComment'
import { VoteBlogComment } from '@/apis/blog/VoteBlogComment'
import { blogCommentContentValue, parseBlogCommentContent } from '@/objects/blog/BlogCommentContent'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogVote } from '@/objects/blog/BlogVote'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

type UseBlogCommentActionsArgs = {
  blog: BlogDetail | null
  setBlog: (blog: BlogDetail | null) => void
}

export function useBlogCommentActions({ blog, setBlog }: UseBlogCommentActionsArgs) {
  const { t } = useI18n()
  const [votingCommentId, setVotingCommentId] = useState<BlogCommentId | null>(null)
  const [commentContent, setCommentContent] = useState('')
  const [replyTargetId, setReplyTargetId] = useState<BlogCommentId | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [isSubmittingReply, setIsSubmittingReply] = useState(false)
  const [commentErrorMessage, setCommentErrorMessage] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<BlogCommentId | null>(null)
  const [editingCommentContent, setEditingCommentContent] = useState('')

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
      setBlog(await sendAPI(new CreateBlogComment(blog.id, { content: parsedContent.value })))
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
      setBlog(await sendAPI(new CreateBlogCommentReply(blog.id, parentCommentId, { content: parsedContent.value })))
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
      setBlog(await sendAPI(new VoteBlogComment(blog.id, commentId, { vote })))
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

    setBlog(await sendAPI(new UpdateBlogComment(blog.id, commentId, { content: parsedContent.value })))
    setEditingCommentId(null)
    setEditingCommentContent('')
  }

  async function removeComment(commentId: BlogCommentId) {
    if (!blog || !window.confirm(t('blog.comment.deleteConfirm'))) {
      return
    }

    setBlog(await sendAPI(new DeleteBlogComment(blog.id, commentId)))
  }

  return {
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
    editingCommentId,
    setEditingCommentId,
    editingCommentContent,
    setEditingCommentContent,
    submitComment,
    submitReply,
    submitCommentVote,
    startEditingComment,
    submitCommentEdit,
    removeComment,
  }
}
