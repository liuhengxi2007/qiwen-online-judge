import { BlogCommentThread } from '@/features/blog/components/blog-comment-thread'
import type { useBlogDetailPageModel } from '@/features/blog/hooks/use-blog-detail-page-model'
import type { BlogDetail } from '@/features/blog/model/response/BlogDetail'
import { useI18n } from '@/shared/i18n/use-i18n'

import { BlogCommentComposer } from './BlogCommentComposer'

type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

type BlogCommentsSectionProps = {
  blog: BlogDetail
  currentUsername: string
  model: BlogDetailPageModel
}

export function BlogCommentsSection({ blog, currentUsername, model }: BlogCommentsSectionProps) {
  const { t } = useI18n()

  return (
    <div className="mt-8 space-y-5">
      <div>
        <h2 className="text-lg font-semibold text-slate-950">{t('blog.comment.heading')}</h2>
        <p className="mt-1 text-sm text-slate-500">{t('blog.comment.description')}</p>
      </div>
      <BlogCommentComposer model={model} />
      <div className="space-y-3">
        <h3 className="text-base font-semibold text-slate-950">{t('blog.comment.heading')}</h3>
        <BlogCommentThread
          comments={blog.comments}
          currentUsername={currentUsername}
          votingCommentId={model.votingCommentId}
          replyTargetId={model.replyTargetId}
          replyContent={model.replyContent}
          isSubmittingReply={model.isSubmittingReply}
          editingCommentId={model.editingCommentId}
          editingCommentContent={model.editingCommentContent}
          commentErrorMessage={model.commentErrorMessage}
          onReplyTargetChange={model.setReplyTargetId}
          onReplyContentChange={model.setReplyContent}
          onEditingCommentIdChange={model.setEditingCommentId}
          onEditingCommentContentChange={model.setEditingCommentContent}
          onSubmitReply={(commentId) => void model.submitReply(commentId)}
          onSubmitCommentVote={(commentId, vote) => void model.submitCommentVote(commentId, vote)}
          onStartEditingComment={model.startEditingComment}
          onSubmitCommentEdit={(commentId) => void model.submitCommentEdit(commentId)}
          onRemoveComment={(commentId) => void model.removeComment(commentId)}
        />
      </div>
    </div>
  )
}
