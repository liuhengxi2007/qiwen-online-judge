import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import type { BlogVote } from '@/objects/blog/BlogVote'

export type BlogCommentThreadState = {
  currentUsername: string
  votingCommentId: BlogCommentId | null
  replyTargetId: BlogCommentId | null
  replyContent: string
  isSubmittingReply: boolean
  editingCommentId: BlogCommentId | null
  editingCommentContent: string
  commentErrorMessage: string
}

export type BlogCommentThreadActions = {
  onReplyTargetChange: (commentId: BlogCommentId | null) => void
  onReplyContentChange: (value: string) => void
  onEditingCommentIdChange: (commentId: BlogCommentId | null) => void
  onEditingCommentContentChange: (value: string) => void
  onSubmitReply: (commentId: BlogCommentId) => void
  onSubmitCommentVote: (commentId: BlogCommentId, vote: BlogVote) => void
  onStartEditingComment: (comment: BlogCommentSummary) => void
  onSubmitCommentEdit: (commentId: BlogCommentId) => void
  onRemoveComment: (commentId: BlogCommentId) => void
}
