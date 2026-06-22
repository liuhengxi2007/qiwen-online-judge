import type { ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { blogCommentContentValue } from '@/objects/blog/BlogCommentContent'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import { blogScoreClassName } from '@/pages/objects/BlogDisplay'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { useI18n } from '@/system/i18n/use-i18n'
import { BlogCommentEditForm } from './BlogCommentEditForm'
import { BlogCommentReplyForm } from './BlogCommentReplyForm'
import type { BlogCommentThreadActions, BlogCommentThreadState } from './BlogCommentThreadTypes'
import { BlogCommentVoteActions } from './BlogCommentVoteActions'

type BlogCommentItemProps = {
  comment: BlogCommentSummary
  depth: number
  children: BlogCommentSummary[]
  state: BlogCommentThreadState
  actions: BlogCommentThreadActions
  renderChild: (comment: BlogCommentSummary, depth: number) => ReactNode
}

export function BlogCommentItem({
  comment,
  depth,
  children,
  state,
  actions,
  renderChild,
}: BlogCommentItemProps) {
  const { t } = useI18n()
  const isEditing =
    state.editingCommentId !== null && blogCommentIdValue(state.editingCommentId) === blogCommentIdValue(comment.id)
  const isReplying =
    state.replyTargetId !== null && blogCommentIdValue(state.replyTargetId) === blogCommentIdValue(comment.id)
  const canManageComment = state.currentUsername === comment.author.username

  return (
    <div
      id={`comment-${blogCommentIdValue(comment.id)}`}
      className={
        depth === 0
          ? 'rounded-3xl border border-slate-200 bg-white p-4'
          : 'ml-6 rounded-3xl border border-slate-200 bg-slate-50 p-4'
      }
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <UserProfileLink className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500" user={comment.author} />
          {isEditing ? (
            <BlogCommentEditForm
              commentId={comment.id}
              content={state.editingCommentContent}
              onContentChange={actions.onEditingCommentContentChange}
              onCancel={() => actions.onEditingCommentIdChange(null)}
              onSubmit={actions.onSubmitCommentEdit}
            />
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
          <BlogCommentVoteActions
            commentId={comment.id}
            viewerVote={comment.viewerVote}
            isVoting={state.votingCommentId === comment.id}
            onVote={actions.onSubmitCommentVote}
          />
          <Button
            type="button"
            size="sm"
            variant="outline"
            className="h-8 rounded-xl border-slate-300 bg-white px-2 text-xs"
            onClick={() => {
              actions.onReplyTargetChange(comment.id)
              actions.onReplyContentChange('')
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
                onClick={() => actions.onStartEditingComment(comment)}
              >
                {t('common.edit')}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="h-8 rounded-xl border-rose-200 bg-white px-2 text-xs text-rose-700"
                onClick={() => actions.onRemoveComment(comment.id)}
              >
                {t('common.delete')}
              </Button>
            </>
          ) : null}
        </div>
      </div>
      {isReplying ? (
        <BlogCommentReplyForm
          commentId={comment.id}
          content={state.replyContent}
          errorMessage={state.commentErrorMessage}
          isSubmitting={state.isSubmittingReply}
          onContentChange={actions.onReplyContentChange}
          onCancel={() => {
            actions.onReplyTargetChange(null)
            actions.onReplyContentChange('')
          }}
          onSubmit={actions.onSubmitReply}
        />
      ) : null}
      {children.length > 0 ? (
        <div className="mt-4 space-y-3">
          {children.map((child) => renderChild(child, depth + 1))}
        </div>
      ) : null}
    </div>
  )
}
