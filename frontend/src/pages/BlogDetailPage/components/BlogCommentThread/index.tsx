import type { ReactNode } from 'react'

import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import { useI18n } from '@/system/i18n/use-i18n'
import { BlogCommentItem } from './BlogCommentItem'
import type { BlogCommentThreadActions, BlogCommentThreadState } from './objects/BlogCommentThreadState'

type BlogCommentThreadProps = BlogCommentThreadState & BlogCommentThreadActions & {
  comments: BlogCommentSummary[]
}

export function BlogCommentThread({ comments, ...props }: BlogCommentThreadProps) {
  const { t } = useI18n()
  const state: BlogCommentThreadState = props
  const actions: BlogCommentThreadActions = props

  function childComments(parentId: BlogCommentId): BlogCommentSummary[] {
    return comments.filter(
      (comment) =>
        comment.parentId !== null && blogCommentIdValue(comment.parentId) === blogCommentIdValue(parentId),
    )
  }

  function renderComment(comment: BlogCommentSummary, depth: number): ReactNode {
    return (
      <BlogCommentItem
        key={comment.id}
        comment={comment}
        depth={depth}
        children={childComments(comment.id)}
        state={state}
        actions={actions}
        renderChild={renderComment}
      />
    )
  }

  if (comments.length === 0) {
    return (
      <p className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-sm text-slate-500">
        {t('blog.comment.empty')}
      </p>
    )
  }

  return (
    <div className="space-y-3">
      {comments.filter((comment) => comment.parentId === null).map((comment) => renderComment(comment, 0))}
    </div>
  )
}
