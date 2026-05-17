import { ThumbsDown, ThumbsUp } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import {
  blogCommentContentValue,
  blogCommentIdValue,
  type BlogCommentId,
  type BlogCommentSummary,
  type BlogVote,
} from '@/features/blog/domain/blog'
import { blogScoreClassName } from '@/features/blog/components/blog-support'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { useI18n } from '@/shared/i18n/use-i18n'

type BlogCommentThreadProps = {
  comments: BlogCommentSummary[]
  currentUsername: string
  votingCommentId: BlogCommentId | null
  replyTargetId: BlogCommentId | null
  replyContent: string
  isSubmittingReply: boolean
  editingCommentId: BlogCommentId | null
  editingCommentContent: string
  commentErrorMessage: string
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

export function BlogCommentThread({
  comments,
  currentUsername,
  votingCommentId,
  replyTargetId,
  replyContent,
  isSubmittingReply,
  editingCommentId,
  editingCommentContent,
  commentErrorMessage,
  onReplyTargetChange,
  onReplyContentChange,
  onEditingCommentIdChange,
  onEditingCommentContentChange,
  onSubmitReply,
  onSubmitCommentVote,
  onStartEditingComment,
  onSubmitCommentEdit,
  onRemoveComment,
}: BlogCommentThreadProps) {
  const { t } = useI18n()

  function isOwnUsername(username: string): boolean {
    return currentUsername === username
  }

  function childComments(parentId: BlogCommentId): BlogCommentSummary[] {
    return comments.filter(
      (comment) =>
        comment.parentId !== null && blogCommentIdValue(comment.parentId) === blogCommentIdValue(parentId),
    )
  }

  function renderComment(comment: BlogCommentSummary, depth: number) {
    const canManageComment = isOwnUsername(comment.author.username)

    return (
      <div
        key={comment.id}
        id={`comment-${blogCommentIdValue(comment.id)}`}
        className={
          depth === 0
            ? 'rounded-3xl border border-slate-200 bg-white p-4'
            : 'ml-6 rounded-3xl border border-slate-200 bg-slate-50 p-4'
        }
      >
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <UserProfileLink
              className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500"
              user={comment.author}
            />
            {editingCommentId !== null &&
            blogCommentIdValue(editingCommentId) === blogCommentIdValue(comment.id) ? (
              <div className="mt-3 space-y-2">
                <Textarea
                  value={editingCommentContent}
                  onChange={(event) => onEditingCommentContentChange(event.target.value)}
                />
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    className="rounded-xl bg-slate-950 text-white"
                    onClick={() => onSubmitCommentEdit(comment.id)}
                  >
                    {t('common.save')}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="rounded-xl"
                    onClick={() => onEditingCommentIdChange(null)}
                  >
                    {t('common.cancel')}
                  </Button>
                </div>
              </div>
            ) : (
              <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-800">
                {blogCommentContentValue(comment.content)}
              </p>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-2 sm:justify-end">
            <span className={`text-xs font-semibold ${blogScoreClassName(comment.score)}`}>
              {t('blog.vote.score', { score: String(comment.score) })}
            </span>
            <Button
              type="button"
              size="sm"
              variant={comment.viewerVote === 'up' ? 'default' : 'outline'}
              className={
                comment.viewerVote === 'up'
                  ? 'h-8 rounded-xl bg-emerald-600 px-2 text-xs text-white hover:bg-emerald-700'
                  : 'h-8 rounded-xl border-emerald-200 bg-white px-2 text-xs text-emerald-700'
              }
              disabled={votingCommentId === comment.id}
              onClick={() => onSubmitCommentVote(comment.id, 'up')}
            >
              <ThumbsUp className="size-3" />
              {t('blog.vote.up')}
            </Button>
            <Button
              type="button"
              size="sm"
              variant={comment.viewerVote === 'down' ? 'default' : 'outline'}
              className={
                comment.viewerVote === 'down'
                  ? 'h-8 rounded-xl bg-rose-600 px-2 text-xs text-white hover:bg-rose-700'
                  : 'h-8 rounded-xl border-rose-200 bg-white px-2 text-xs text-rose-700'
              }
              disabled={votingCommentId === comment.id}
              onClick={() => onSubmitCommentVote(comment.id, 'down')}
            >
              <ThumbsDown className="size-3" />
              {t('blog.vote.down')}
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="h-8 rounded-xl border-slate-300 bg-white px-2 text-xs"
              onClick={() => {
                onReplyTargetChange(comment.id)
                onReplyContentChange('')
              }}
            >
              {t('blog.comment.reply')}
            </Button>
            {canManageComment ? (
              <>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="h-8 rounded-xl border-slate-300 bg-white px-2 text-xs"
                  onClick={() => onStartEditingComment(comment)}
                >
                  {t('common.edit')}
                </Button>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="h-8 rounded-xl border-rose-200 bg-white px-2 text-xs text-rose-700"
                  onClick={() => onRemoveComment(comment.id)}
                >
                  {t('common.delete')}
                </Button>
              </>
            ) : null}
          </div>
        </div>
        {replyTargetId !== null &&
        blogCommentIdValue(replyTargetId) === blogCommentIdValue(comment.id) ? (
          <div className="mt-4 space-y-3 rounded-2xl border border-slate-200 bg-white p-3">
            <Textarea
              value={replyContent}
              className="min-h-24"
              onChange={(event) => onReplyContentChange(event.target.value)}
            />
            {commentErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{commentErrorMessage}</AlertDescription>
              </Alert>
            ) : null}
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                size="sm"
                disabled={isSubmittingReply}
                className="rounded-xl bg-slate-950 text-white hover:bg-slate-800"
                onClick={() => onSubmitReply(comment.id)}
              >
                {isSubmittingReply ? t('blog.comment.submitting') : t('blog.comment.replySubmit')}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="rounded-xl border-slate-300 bg-white"
                onClick={() => {
                  onReplyTargetChange(null)
                  onReplyContentChange('')
                }}
              >
                {t('common.cancel')}
              </Button>
            </div>
          </div>
        ) : null}
        {childComments(comment.id).length > 0 ? (
          <div className="mt-4 space-y-3">
            {childComments(comment.id).map((child) => renderComment(child, depth + 1))}
          </div>
        ) : null}
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {comments.length === 0 ? (
        <p className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-sm text-slate-500">
          {t('blog.comment.empty')}
        </p>
      ) : (
        <div className="space-y-3">
          {comments.filter((comment) => comment.parentId === null).map((comment) => renderComment(comment, 0))}
        </div>
      )}
    </div>
  )
}
